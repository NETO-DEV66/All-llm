package com.netodev66.alllm.mcp

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

private class RecordingHttpLayer(
    var response: McpHttpResponse = McpHttpResponse(200, "{}"),
) : McpHttpLayer {
    var lastUrl: String? = null
    var lastHeaders: Map<String, String> = emptyMap()
    var lastBody: String? = null
    var calls = 0

    override fun post(url: String, headers: Map<String, String>, body: String, timeoutMs: Long): McpHttpResponse {
        calls++
        lastUrl = url
        lastHeaders = headers
        lastBody = body
        return response
    }
}

class KeiroLabsSearchHandlerTest {

    private val config = mapOf(
        KeiroLabsSearchHandler.CONFIG_API_KEY to "test-key",
        KeiroLabsSearchHandler.CONFIG_BASE_URL to "https://example.test/api",
    )

    @Test
    fun `advertises the web_search tool with a query schema`() {
        val tools = KeiroLabsSearchHandler().tools()
        assertEquals(1, tools.size)
        assertEquals("web_search", tools.first().name)
        assertTrue(tools.first().inputSchema.getJSONObject("properties").has("query"))
    }

    @Test
    fun `posts the query to the search endpoint with a bearer key`() {
        val http = RecordingHttpLayer(
            McpHttpResponse(200, """{"query":"kotlin","results":[{"title":"T","url":"https://u","snippet":"S"}]}"""),
        )
        val outcome = KeiroLabsSearchHandler(http).call("web_search", JSONObject("""{"query":"kotlin"}"""), config)

        assertEquals(false, outcome.isError)
        assertEquals("https://example.test/api/search", http.lastUrl)
        assertEquals("Bearer test-key", http.lastHeaders["Authorization"])
        assertEquals("kotlin", JSONObject(http.lastBody!!).getString("query"))
        assertTrue(outcome.text.contains("https://u"))
        assertTrue(outcome.text.contains("T"))
    }

    @Test
    fun `a blank count is not sent, so the API keeps its default`() {
        val http = RecordingHttpLayer(McpHttpResponse(200, """{"query":"q","results":[]}"""))
        KeiroLabsSearchHandler(http).call("web_search", JSONObject("""{"query":"q"}"""), config)
        assertTrue(!JSONObject(http.lastBody!!).has("count"))
    }

    @Test
    fun `a missing key fails with a message that says where to add it`() {
        val http = RecordingHttpLayer()
        try {
            KeiroLabsSearchHandler(http).call("web_search", JSONObject("""{"query":"q"}"""), emptyMap())
            fail("expected a configuration error")
        } catch (e: McpConfigurationException) {
            assertTrue(e.message!!.contains("API key"))
        }
        assertEquals(0, http.calls)
    }

    @Test
    fun `a blank query fails instead of searching for nothing`() {
        val http = RecordingHttpLayer()
        try {
            KeiroLabsSearchHandler(http).call("web_search", JSONObject("""{"query":"  "}"""), config)
            fail("expected a configuration error")
        } catch (e: McpConfigurationException) {
            assertTrue(e.message!!.contains("query"))
        }
        assertEquals(0, http.calls)
    }

    @Test
    fun `a rejected key surfaces as an auth error, not a generic transport error`() {
        val http = RecordingHttpLayer(McpHttpResponse(401, """{"error":"Invalid API key"}"""))
        try {
            KeiroLabsSearchHandler(http).call("web_search", JSONObject("""{"query":"q"}"""), config)
            fail("expected an auth error")
        } catch (e: McpAuthException) {
            assertTrue(e.message!!.contains("401"))
        }
    }

    @Test
    fun `an HTML error page is reported as such rather than shown as results`() {
        val http = RecordingHttpLayer(McpHttpResponse(200, "<html>gateway</html>"))
        val outcome = KeiroLabsSearchHandler(http).call("web_search", JSONObject("""{"query":"q"}"""), config)
        assertEquals(false, outcome.isError)
        assertTrue(outcome.text.contains("non-JSON"))
    }

    @Test
    fun `the default handler is the one the registry resolves by id`() {
        assertEquals("keiro-search", KeiroLabsSearchHandler.default.id)
        assertTrue(McpNativeHandlers.find("keiro-search") === KeiroLabsSearchHandler.default)
    }
}
