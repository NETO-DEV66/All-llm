package com.netodev66.alllm.mcp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The reported bug: installing a built-in MCP server showed "installed" but the
 * server never ran. The template stored its whole command line in `command`,
 * which the launcher execs as a single binary, so the process could not start.
 */
class McpCommandLineTest {

    @Test
    fun `template command line splits into executable and args`() {
        val (exe, args) = splitMcpCommandLine("npx -y @modelcontextprotocol/server-sqlite")
        assertEquals("npx", exe)
        assertEquals(listOf("-y", "@modelcontextprotocol/server-sqlite"), args)
    }

    @Test
    fun `extra whitespace is collapsed`() {
        val (exe, args) = splitMcpCommandLine("  npx   -y   server  ")
        assertEquals("npx", exe)
        assertEquals(listOf("-y", "server"), args)
    }

    @Test
    fun `single executable yields no args`() {
        val (exe, args) = splitMcpCommandLine("my-mcp-server")
        assertEquals("my-mcp-server", exe)
        assertTrue(args.isEmpty())
    }

    @Test
    fun `quoted path with spaces stays one token`() {
        val (exe, args) = splitMcpCommandLine("\"/opt/my tools/server\" --port 8080")
        assertEquals("/opt/my tools/server", exe)
        assertEquals(listOf("--port", "8080"), args)
    }

    @Test
    fun `blank input yields blank executable`() {
        val (exe, args) = splitMcpCommandLine("   ")
        assertEquals("", exe)
        assertTrue(args.isEmpty())
    }

    @Test
    fun `every built-in template splits into a real executable`() {
        McpBuiltInCatalogTemplates.forEach { template ->
            val (exe, embedded) = splitMcpCommandLine(template.defaultCommand)
            assertTrue("template ${template.id} has no executable", exe.isNotBlank())
            assertFalse("template ${template.id} left args in the command", exe.contains(' '))
            assertEquals(
                "template ${template.id} lost args while splitting",
                template.defaultCommand,
                (listOf(exe) + embedded).joinToString(" "),
            )
        }
    }

    @Test
    fun `legacy server holding a whole command line is repaired`() {
        val legacy = McpServerInfo(
            id = "sqlite",
            name = "SQLite Server",
            command = "npx -y @modelcontextprotocol/server-sqlite",
            args = listOf("--db", "workspace.db"),
        )
        val repaired = normalizeServerCommandLine(legacy)
        assertEquals("npx", repaired.command)
        assertEquals(listOf("-y", "@modelcontextprotocol/server-sqlite", "--db", "workspace.db"), repaired.args)
    }

    @Test
    fun `already correct server is untouched`() {
        val server = McpServerInfo(id = "x", name = "X", command = "node", args = listOf("server.js"))
        assertEquals(server, normalizeServerCommandLine(server))
    }

    @Test
    fun `single executable with a space in its path is not split`() {
        val dir = File(System.getProperty("java.io.tmpdir"), "mcp cmdline " + System.nanoTime())
        assertTrue(dir.mkdirs())
        try {
            val spaced = File(dir, "my server")
            spaced.writeText("")
            val server = McpServerInfo(id = "x", name = "X", command = spaced.absolutePath)
            assertEquals(server, normalizeServerCommandLine(server))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `template install splits the command so the server can start`() {
        val dir = File(System.getProperty("java.io.tmpdir"), "mcp_template_" + System.nanoTime())
        try {
            val registry = McpRegistry(File(dir, "registry"))
            val installed = registry.installFromTemplate("sqlite")
            assertEquals("npx", installed.command)
            assertFalse("command must be a single executable", installed.command.contains(' '))
            assertEquals(listOf("-y", "@executeautomation/database-server", "--sqlite", "workspace.db"), installed.args)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `persisted legacy config is repaired on load`() {
        val dir = File(System.getProperty("java.io.tmpdir"), "mcp_legacy_" + System.nanoTime())
        try {
            val registryDir = File(dir, "registry")
            registryDir.mkdirs()
            File(registryDir, "mcp_servers.json").writeText(
                """
                [{"id":"sqlite","name":"SQLite","command":"npx -y @modelcontextprotocol/server-sqlite",
                  "args":["--db","workspace.db"],"enabled":true,"isBuiltIn":true}]
                """.trimIndent(),
            )
            val reloaded = McpRegistry(registryDir).getServer("sqlite")
            assertEquals("npx", reloaded!!.command)
            assertEquals(listOf("-y", "@modelcontextprotocol/server-sqlite", "--db", "workspace.db"), reloaded.args)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `installServer splits a whole command line typed into the custom dialog`() {
        val dir = File(System.getProperty("java.io.tmpdir"), "mcp_custom_" + System.nanoTime())
        try {
            val registry = McpRegistry(File(dir, "registry"))
            // The add-custom-server dialog's field invites a full command line.
            // Stored whole, the launcher would exec one binary named
            // "npx -y @scope/server" and the server would never start.
            val installed = registry.installServer(
                McpServerInfo(
                    id = "custom-demo",
                    name = "Demo",
                    command = "npx -y @scope/server",
                    enabled = true,
                    isBuiltIn = false,
                ),
            )
            assertEquals("npx", installed.command)
            assertEquals(listOf("-y", "@scope/server"), installed.args)

            // The repair must survive a reload, or the next launch re-breaks it.
            val reloaded = McpRegistry(File(dir, "registry")).getServer("custom-demo")!!
            assertEquals("npx", reloaded.command)
            assertEquals(listOf("-y", "@scope/server"), reloaded.args)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `installServer keeps a quoted executable path whole`() {
        val dir = File(System.getProperty("java.io.tmpdir"), "mcp_quoted_" + System.nanoTime())
        try {
            val registry = McpRegistry(File(dir, "registry"))
            val installed = registry.installServer(
                McpServerInfo(
                    id = "custom-quoted",
                    name = "Quoted",
                    command = "\"/opt/my tools/server\" --port 8080",
                    enabled = true,
                    isBuiltIn = false,
                ),
            )
            assertEquals("/opt/my tools/server", installed.command)
            assertEquals(listOf("--port", "8080"), installed.args)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `tokenizer keeps quoted arguments whole`() {
        assertEquals(listOf("--dir", "/my data", "--verbose"), tokenizeMcpCommandLine("--dir \"/my data\" --verbose"))
    }
}
