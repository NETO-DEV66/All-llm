package com.netodev66.alllm.mcp

import com.netodev66.alllm.runtime.InstalledRuntime
import com.netodev66.alllm.runtime.NativeSpawnProcess
import com.netodev66.alllm.runtime.RuntimeInstaller
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Reads newline-delimited text from a file that another process keeps appending
 * to.
 *
 * A guest process launched by [RuntimeInstaller.process] writes stdout to a file
 * rather than a pipe, so an ordinary [java.io.BufferedReader] hits end-of-file at
 * the current write position and reports a false disconnect. This tails the file
 * instead, which is what a long-lived MCP server needs.
 */
internal class GrowingFileLineReader(
    private val file: File,
    private val isWriterAlive: () -> Boolean,
    private val pollIntervalMs: Long = 25,
) {
    private val lines = LinkedBlockingQueue<String>()
    private val pending = ByteArrayOutputStream()
    private var offset = 0L

    private val pump = Thread({
        while (true) {
            runCatching { readAvailable() }
            if (!isWriterAlive() && file.length() <= offset && pending.size() == 0) {
                // Drain anything the writer managed to flush after our last read.
                runCatching { readAvailable() }
                if (file.length() <= offset && pending.size() == 0) return@Thread
            }
            Thread.sleep(pollIntervalMs)
        }
    }, "mcp-guest-stdout").apply { isDaemon = true; start() }

    /** Returns the next complete line, or null on timeout or end of stream. */
    fun pollLine(timeoutMs: Long): String? {
        lines.poll(timeoutMs, TimeUnit.MILLISECONDS)?.let { return it }
        return null
    }

    fun stop() {
        pump.interrupt()
    }

    private fun readAvailable() {
        val length = file.length()
        if (length <= offset) return
        val chunk = ByteArray((length - offset).toInt())
        java.io.RandomAccessFile(file, "r").use { raf ->
            raf.seek(offset)
            raf.readFully(chunk)
        }
        offset = length
        pending.write(chunk)
        emitCompleteLines()
    }

    private fun emitCompleteLines() {
        val bytes = pending.toByteArray()
        var start = 0
        for (i in bytes.indices) {
            if (bytes[i] != '\n'.code.toByte()) continue
            val end = if (i > start && bytes[i - 1] == '\r'.code.toByte()) i - 1 else i
            // Decode only whole lines, so a multi-byte character split across two
            // reads is never turned into a replacement character.
            val line = String(bytes, start, end - start, Charsets.UTF_8)
            start = i + 1
            if (line.isNotBlank()) lines.offer(line)
        }
        if (start == 0) return
        pending.reset()
        if (start < bytes.size) pending.write(bytes, start, bytes.size - start)
    }
}

/**
 * An MCP stdio session backed by a process running inside the app's Linux
 * runtime rather than directly on Android.
 */
internal class RuntimeMcpManagedProcess(
    private val process: NativeSpawnProcess,
    outputFile: File,
    private val stderrFile: File,
) : McpManagedProcess {

    private val reader = GrowingFileLineReader(outputFile, isWriterAlive = { process.isAlive })

    override fun writeLine(line: String) {
        process.outputStream.write(line.toByteArray(Charsets.UTF_8))
        process.outputStream.write('\n'.code)
        process.outputStream.flush()
    }

    override fun readLine(timeoutMs: Long): String? = reader.pollLine(timeoutMs)

    override fun drainStderr(): List<String> =
        runCatching { stderrFile.readLines().takeLast(200) }.getOrDefault(emptyList())

    override fun isAlive(): Boolean = process.isAlive

    override fun destroyGracefully(graceMs: Long): Boolean {
        reader.stop()
        process.destroy()
        return runCatching { process.waitFor(graceMs, TimeUnit.MILLISECONDS) }.getOrDefault(false)
    }

    override fun destroyForcibly() {
        reader.stop()
        process.destroyForcibly()
    }

    override fun exitCode(): Int? = runCatching { process.exitValue() }.getOrNull()
}

/**
 * Commands that only exist inside the Linux runtime. The device has no Node on
 * Android's own `PATH`, so a server declared as `npx …` could never start; every
 * built-in catalog template is one of these.
 */
internal val guestOnlyCommands = setOf("npx", "npm", "node", "corepack", "npx.cmd", "node.exe")

internal fun isGuestOnlyCommand(command: String): Boolean {
    val name = command.substringAfterLast('/').substringAfterLast('\\').lowercase()
    return name in guestOnlyCommands
}

/**
 * Starts a stdio MCP server inside the app's PRoot runtime.
 *
 * Built-in catalog servers run through `npx`, which lives in the runtime's
 * Ubuntu rootfs and not on the device. Spawning the bare command on Android made
 * every built-in server register, report "installed", and then fail on its first
 * call. This routes those commands through the runtime, and falls back to a plain
 * Android process for a custom server that names a real host binary.
 */
class McpRuntimeProcessFactory(
    private val installer: RuntimeInstaller,
    private val runtime: () -> InstalledRuntime?,
    private val workspace: () -> File,
    private val fallback: McpProcessFactory = RealMcpProcessFactory(),
) : McpProcessFactory {

    override fun start(
        command: String,
        args: List<String>,
        env: Map<String, String>,
        workingDir: File?,
    ): McpManagedProcess {
        val installed = if (isGuestOnlyCommand(command)) runtime() else null
        if (installed == null) return fallback.start(command, args, env, workingDir)

        val stderrFile = File(installed.rootfs, "tmp/mcp-stderr-${System.nanoTime()}.log")
        stderrFile.parentFile?.mkdirs()
        val outputFile = File(
            installer.outputDirectory(),
            "mcp-${command.substringAfterLast('/')}-${System.nanoTime()}.log",
        )
        // The runtime merges stderr into stdout, and a single stray warning line
        // would break the JSON-RPC stream. Redirect stderr to a file of its own so
        // stdout carries nothing but protocol messages.
        val script = "exec \"\$0\" \"\$@\" 2>>'$stderrFile'"

        val process = installer.process(
            proot = installed.proot,
            rootfs = installed.rootfs,
            workspace = workingDir ?: workspace(),
            environment = env + npxNonInteractiveEnv,
            guestCommand = listOf("bash", "-c", script, command) + args,
            outputFile = outputFile,
        ) as? NativeSpawnProcess
            ?: throw McpTransportException("MCP server '$command' could not start inside the runtime")

        return RuntimeMcpManagedProcess(process, outputFile, stderrFile)
    }

    private companion object {
        /**
         * `npx` prompts "Ok to proceed?" on a package's first run. A prompt on
         * stdout corrupts the protocol stream, so the prompt is suppressed and
         * the answer supplied up front.
         */
        val npxNonInteractiveEnv = mapOf(
            "npm_config_yes" to "true",
            "npm_config_fund" to "false",
            "npm_config_audit" to "false",
            "npm_config_update_notifier" to "false",
            "npm_config_loglevel" to "error",
            "CI" to "true",
        )
    }
}
