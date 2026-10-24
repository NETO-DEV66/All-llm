package com.netodev66.alllm.ui.coordinators

import com.netodev66.alllm.marketplace.DeveloperMarketplaceCatalog
import com.netodev66.alllm.mcp.McpBuiltInCatalogTemplates
import com.netodev66.alllm.marketplace.MarketplaceCategory
import com.netodev66.alllm.marketplace.MarketplaceItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * QA C9: installing a marketplace MCP item must register it as a usable
 * MCP server. The item -> server mapping is the new code that decides
 * what gets registered, so it is tested directly.
 */
class MarketplaceCoordinatorMcpMappingTest {

    private fun mcpItem(installUrl: String) = MarketplaceItem(
        id = "pg-mcp",
        name = "Postgres MCP",
        category = MarketplaceCategory.MCP_SERVERS,
        author = "MCP",
        description = "Postgres MCP server.",
        installUrl = installUrl,
    )

    @Test
    fun `npx command maps to server with split command and args`() {
        val server = marketplaceItemToMcpServer(mcpItem("npx -y @modelcontextprotocol/server-postgres"), "marketplace-pg-mcp")
        assertEquals("marketplace-pg-mcp", server!!.id)
        assertEquals("Postgres MCP", server.name)
        assertEquals("npx", server.command)
        assertEquals(listOf("-y", "@modelcontextprotocol/server-postgres"), server.args)
        assertTrue(server.enabled)
        assertFalse(server.isBuiltIn)
    }

    @Test
    fun `single command with no args maps to empty args`() {
        val server = marketplaceItemToMcpServer(mcpItem("my-mcp-server"), "marketplace-pg-mcp")
        assertEquals("my-mcp-server", server!!.command)
        assertTrue(server.args.isEmpty())
    }

    @Test
    fun `blank install value maps to null`() {
        assertNull(marketplaceItemToMcpServer(mcpItem("   "), "marketplace-pg-mcp"))
    }

    @Test
    fun `http url install value maps to null`() {
        assertNull(marketplaceItemToMcpServer(mcpItem("https://example.com/pkg.zip"), "marketplace-pg-mcp"))
        assertNull(marketplaceItemToMcpServer(mcpItem("http://example.com/pkg.zip"), "marketplace-pg-mcp"))
    }

    @Test
    fun `extra whitespace is collapsed`() {
        val server = marketplaceItemToMcpServer(mcpItem("  npx   -y   server  "), "marketplace-pg-mcp")
        assertEquals("npx", server!!.command)
        assertEquals(listOf("-y", "server"), server.args)
    }

    @Test
    fun `caller supplied id wins so catalog items share one identity`() {
        val server = marketplaceItemToMcpServer(mcpItem("npx -y server-sqlite"), "sqlite")
        assertEquals("sqlite", server!!.id)
    }

    @Test
    fun `every built-in catalog listing maps to a runnable server`() {
        // The reported bug was "installing says installed but nothing happens".
        // Every listing the marketplace derives from McpBuiltInCatalogTemplates
        // must survive the item -> server mapping, or install silently registers
        // nothing and the card still reads "Installed".
        val listings = DeveloperMarketplaceCatalog.curatedCatalog
            .filter { it.category == MarketplaceCategory.MCP_SERVERS }
        assertTrue("catalog must expose MCP listings", listings.isNotEmpty())
        listings.forEach { item ->
            val server = marketplaceItemToMcpServer(item, item.id)
            assertNotNull("no server registered for ${item.id}", server)
            assertEquals("server id must match the listing id for ${item.id}", item.id, server!!.id)
            // A command holding a whole line can never be exec'd: this is the
            // difference between "says installed" and "actually starts".
            assertFalse("command must be one executable for ${item.id}", server.command.contains(' '))
            assertTrue("command must not be blank for ${item.id}", server.command.isNotBlank())
            assertTrue("server must be enabled for ${item.id}", server.enabled)
            val template = McpBuiltInCatalogTemplates.find { "catalog-mcp-${it.id}" == item.id }
            if (template?.nativeHandler != null) {
                // A native server runs in-app, so it has no arguments to exec. What
                // must survive the mapping is the handler, or install registers a
                // server that can never dispatch a tool.
                assertEquals(
                    "the in-app handler must survive the mapping for ${item.id}",
                    template.nativeHandler,
                    server.nativeHandler,
                )
            } else {
                assertTrue("args must be populated for ${item.id}", server.args.isNotEmpty())
            }
        }
    }

    @Test
    fun `catalog listing id resolves back to its template id`() {
        // The uninstall path mirrors this lookup, so a drift here would leave a
        // registered server behind after uninstalling the listing.
        McpBuiltInCatalogTemplates.forEach { template ->
            val listing = requireNotNull(
                DeveloperMarketplaceCatalog.curatedCatalog.find { it.id == "catalog-mcp-${template.id}" }
            ) { "listing missing for template ${template.id}" }
            val resolved = requireNotNull(
                McpBuiltInCatalogTemplates.find { "catalog-mcp-${it.id}" == listing.id }
            ) { "listing ${listing.id} must resolve to a template id" }
            assertEquals(template.id, resolved.id)
            val server = requireNotNull(marketplaceItemToMcpServer(listing, resolved.id)) {
                "listing ${listing.id} must map to a server"
            }
            // The launcher execs `command` as one binary, so the template's full
            // command line must come back out as executable + args.
            assertEquals(
                (listOf(template.defaultCommand) + template.defaultArgs).joinToString(" "),
                (listOf(server.command) + server.args).joinToString(" "),
            )
            assertEquals(template.defaultCommand.substringBefore(' '), server.command)
            assertTrue(
                "args must include the template args for ${template.id}",
                server.args.containsAll(template.defaultArgs),
            )
        }
    }
}
