package com.netodev66.alllm.mcp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream

/**
 * The built-in MCP catalog launches every server through `npx`, which lives in
 * the PRoot rootfs. `isGuestOnlyCommand` is what decides a server must be routed
 * through the runtime instead of spawned on Android; getting it wrong is exactly
 * the "installed but never runs" bug.
 */
class McpRuntimeProcessTest {

    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `npx and friends are recognised as runtime-only commands`() {
        listOf("npx", "npm", "node", "corepack").forEach { command ->
            assertTrue("$command must route through the runtime", isGuestOnlyCommand(command))
        }
    }

    @Test
    fun `a path to a runtime-only command still routes through the runtime`() {
        assertTrue(isGuestOnlyCommand("/usr/local/bin/npx"))
        assertTrue(isGuestOnlyCommand("C:\\nodejs\\npx.cmd"))
        assertTrue(isGuestOnlyCommand("/usr/bin/NODE"))
    }

    @Test
    fun `a real host binary keeps spawning directly`() {
        listOf("/system/bin/sh", "python3", "/data/data/com.netodev66.alllm/native", "busybox")
            .forEach { command ->
                assertFalse("$command must not be rerouted", isGuestOnlyCommand(command))
            }
    }

    @Test
    fun `every built-in catalog template is either native or runtime-routed`() {
        // A template that is neither would register a server that can never start,
        // which is the bug this whole file guards.
        val unrunnable = McpBuiltInCatalogTemplates.filter { template ->
            template.nativeHandler == null &&
                !isGuestOnlyCommand(template.defaultCommand.substringBefore(' '))
        }
        assertEquals(emptyList<McpCatalogTemplate>(), unrunnable)
    }

    @Test
    fun `the line reader returns whole lines and holds a partial one back`() {
        val file = temp.newFile("stdout.log")
        file.appendText("{\"id\":1}\n{\"id\":2}\n{\"partial")

        val reader = GrowingFileLineReader(file, isWriterAlive = { true })
        assertEquals("{\"id\":1}", reader.pollLine(1_000))
        assertEquals("{\"id\":2}", reader.pollLine(1_000))
        // "{"partial" has no newline yet, so it must not be handed out.
        assertNull(reader.pollLine(200))
        reader.stop()
    }

    @Test
    fun `the line reader picks up a line appended after it started`() {
        val file = temp.newFile("stdout.log")
        val reader = GrowingFileLineReader(file, isWriterAlive = { true })
        assertNull(reader.pollLine(100))

        FileOutputStream(file, true).use { it.write("{\"id\":9}\n".toByteArray()) }

        assertEquals("{\"id\":9}", reader.pollLine(2_000))
        reader.stop()
    }

    @Test
    fun `a multi-byte character split across two writes is decoded intact`() {
        val file = temp.newFile("stdout.log")
        val reader = GrowingFileLineReader(file, isWriterAlive = { true })
        // "₹" is three bytes; write it one byte at a time with a newline after.
        val encoded = "₹\n".toByteArray(Charsets.UTF_8)
        FileOutputStream(file, true).use { out ->
            encoded.dropLast(1).forEach { out.write(byteArrayOf(it)); out.flush() }
        }
        assertNull("an incomplete character must not be emitted", reader.pollLine(200))
        FileOutputStream(file, true).use { it.write(encoded.last().toInt()) }

        assertEquals("₹", reader.pollLine(2_000))
        reader.stop()
    }

    @Test
    fun `carriage returns are stripped so Windows line endings do not corrupt JSON`() {
        val file = temp.newFile("stdout.log")
        file.appendText("{\"id\":1}\r\n")
        val reader = GrowingFileLineReader(file, isWriterAlive = { true })
        assertEquals("{\"id\":1}", reader.pollLine(1_000))
        reader.stop()
    }

    @Test
    fun `the reader stops once the writer is gone and its output is drained`() {
        val file = temp.newFile("stdout.log")
        file.appendText("{\"id\":1}\n")
        val reader = GrowingFileLineReader(file, isWriterAlive = { false })

        assertEquals("{\"id\":1}", reader.pollLine(2_000))
        assertNull("a finished writer must not leave the reader blocking", reader.pollLine(500))
        reader.stop()
    }

    @Test
    fun `a runtime-only server without the runtime fails with an actionable message`() {
        // The alternative is ProcessBuilder throwing a bare "Cannot run program
        // npx" on a device that has no Node, which reads as a broken app rather
        // than a missing runtime install.
        val message = missingRuntimeMessage("npx")
        assertTrue("npx must explain the missing runtime, got $message", message != null)
        assertTrue(message!!.contains("Linux runtime"))
        assertTrue(message.contains("Settings > Runtime"))
    }

    @Test
    fun `a host binary needs no runtime, so no message is produced`() {
        // Only guest-only commands need the runtime; a custom server naming a real
        // host binary must keep working with the runtime absent.
        assertNull(missingRuntimeMessage("busybox"))
        assertNull(missingRuntimeMessage("/system/bin/sh"))
    }

    @Test
    fun `a runtime-only command reached by path names the bare command in the message`() {
        val message = missingRuntimeMessage("/usr/local/bin/npx")
        assertTrue("message should name the bare command: $message", message!!.contains("'npx'"))
    }
}
