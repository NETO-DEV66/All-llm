package com.netodev66.alllm.mcp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the npm package each built-in template installs.
 *
 * Four of the nine catalog rows used to name packages that could not install:
 * `@modelcontextprotocol/server-sqlite` and `@modelcontextprotocol/server-fetch`
 * were never published at all (npx exits 404), and `server-postgres` /
 * `server-puppeteer` are deprecated upstream and fail on start. The card still
 * said "Installed", because registering a server and downloading a package are
 * different things — so the bug was invisible until the server was started.
 *
 * The expected list is literal on purpose. It is the record of what was checked
 * against the registry by hand; a future edit that swaps a package has to come
 * back here and say so.
 */
class McpCatalogPackageTest {

    /** template id -> npm package spec that must resolve and start. */
    private val verifiedPackages = mapOf(
        "sqlite" to "@executeautomation/database-server",
        "filesystem" to "@modelcontextprotocol/server-filesystem",
        "github" to "@modelcontextprotocol/server-github",
        "postgres" to "@henkey/postgres-mcp-server",
        "fetch" to "fetch-mcp",
        "puppeteer" to "@playwright/mcp",
        "memory" to "@modelcontextprotocol/server-memory",
        "sequential-thinking" to "@modelcontextprotocol/server-sequential-thinking",
    )

    /** These never resolved; naming one again reintroduces the silent failure. */
    private val deadPackages = setOf(
        "@modelcontextprotocol/server-sqlite",
        "@modelcontextprotocol/server-fetch",
        "@modelcontextprotocol/server-postgres",
        "@modelcontextprotocol/server-puppeteer",
    )

    private fun template(id: String) = McpBuiltInCatalogTemplates.first { it.id == id }

    @Test
    fun `every built-in template installs a package that exists`() {
        verifiedPackages.forEach { (id, pkg) ->
            val command = template(id).defaultCommand
            assertTrue(
                "template '$id' must install $pkg, but installs '$command'",
                command.contains(pkg),
            )
        }
    }

    @Test
    fun `no template points at a package known to be dead`() {
        McpBuiltInCatalogTemplates.forEach { template ->
            deadPackages.forEach { dead ->
                assertFalse(
                    "template '${template.id}' points at $dead, which cannot install",
                    template.defaultCommand.contains(dead),
                )
            }
        }
    }

    @Test
    fun `every template id in the catalog is covered by this test`() {
        val npxTemplates = McpBuiltInCatalogTemplates.filter { it.defaultCommand.startsWith("npx") }
        assertEquals(
            "a template was added or removed without checking its package",
            verifiedPackages.keys,
            npxTemplates.map { it.id }.toSet(),
        )
    }

    @Test
    fun `a template that needs a secret declares it`() {
        // GitHub and Postgres are unusable without a credential, and the installer
        // only prompts for the names listed here.
        assertEquals(listOf("GITHUB_PERSONAL_ACCESS_TOKEN"), template("github").requiredEnv)
        assertEquals(listOf("POSTGRES_CONNECTION_STRING"), template("postgres").requiredEnv)
    }
}
