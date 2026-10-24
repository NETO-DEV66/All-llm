package com.netodev66.alllm.ui.coordinators

import com.netodev66.alllm.marketplace.MarketplaceCategory
import com.netodev66.alllm.marketplace.MarketplaceItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
