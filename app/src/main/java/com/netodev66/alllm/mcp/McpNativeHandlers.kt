package com.netodev66.alllm.mcp

import org.json.JSONArray
import org.json.JSONObject

/**
 * Tools that live inside the app rather than in a child process.
 *
 * This exists because the built-in catalog cannot use `npx`: Android has no Node
 * on PATH, so every `npx`-based template registered, showed "installed", and then
 * failed to start forever. A server whose tools are plain REST calls belongs
 * in-process, where it can actually run and where its key stays in the encrypted
 * credential store instead of a process environment.
 *
 * Handlers are pure: they receive a resolved config and a parsed argument object
 * and return text. All permission gating, auditing and caching stays in
 * [McpToolRegistry], so a handler cannot bypass those by accident.
 */
interface McpNativeHandler {
    val id: String
    fun tools(): List<McpToolDefinition>

    /**
     * @param config resolved non-secret env plus secrets from [McpCredentialStore].
     * @throws McpConfigurationException when required configuration is missing.
     */
    fun call(toolName: String, arguments: JSONObject, config: Map<String, String>): McpToolCallOutcome
}

/**
 * The in-app MCP servers, keyed by the id persisted in [McpServerInfo.nativeHandler].
 * Registering a new REST-backed server means adding one entry here and one catalog
 * template; no transport or registry change is needed.
 */
object McpNativeHandlers {
    val all: List<McpNativeHandler> = listOf(KeiroLabsSearchHandler.default)

    fun find(id: String?): McpNativeHandler? = id?.let { wanted -> all.firstOrNull { it.id == wanted } }
}

/** A tool argument object that fails loudly instead of reading nulls as empty strings. */
internal fun JSONObject.requiredString(name: String): String {
    val value = optString(name, "")
    if (value.isBlank()) throw McpConfigurationException("Missing required argument '$name'")
    return value
}

/**
 * KieroLabs web search — a REST search API, used in place of the Brave Search
 * template, which needs a Brave subscription.
 *
 * The API is `POST {base}/search` with `Authorization: Bearer <key>` and a JSON
 * body, answering `{"query":…,"results":[{title,url,snippet,position}]}`.
 */
class KeiroLabsSearchHandler(
    private val httpLayer: McpHttpLayer = UrlConnectionMcpHttpLayer(),
) : McpNativeHandler {

    override val id = HANDLER_ID

    companion object {
        const val HANDLER_ID = "keiro-search"
        const val CONFIG_BASE_URL = "KEIRO_BASE_URL"
        const val CONFIG_API_KEY = "KEIRO_API_KEY"
        private const val DEFAULT_BASE_URL = "https://kierolabs.space/api"

        /** The handler the registry runs; keeps one HTTP layer so connections are reused. */
        val default: McpNativeHandler = KeiroLabsSearchHandler()
    }

    override fun tools(): List<McpToolDefinition> = listOf(
        McpToolDefinition(
            name = "web_search",
            description = "Search the web and return ranked results with titles, URLs and snippets.",
            inputSchema = JSONObject(
                """
                {
                  "type": "object",
                  "properties": {
                    "query": {"type": "string", "description": "The search query."},
                    "count": {"type": "integer", "description": "How many results to return (default 5)."}
                  },
                  "required": ["query"]
                }
                """.trimIndent(),
            ),
        ),
    )

    override fun call(
        toolName: String,
        arguments: JSONObject,
        config: Map<String, String>,
    ): McpToolCallOutcome {
        if (toolName != "web_search") {
            return McpToolCallOutcome(isError = true, text = "Unknown tool '$toolName'")
        }
        val query = arguments.requiredString("query")
        val baseUrl = config[CONFIG_BASE_URL]?.takeIf { it.isNotBlank() }?.trimEnd('/') ?: DEFAULT_BASE_URL
        val apiKey = config[CONFIG_API_KEY]?.takeIf { it.isNotBlank() }
            ?: throw McpConfigurationException(
                "KieroLabs search needs an API key. Add it in Skills > MCP Servers > KieroLabs Web Search."
            )

        val body = JSONObject().apply {
            put("query", query)
            // Only forward a count the caller asked for; otherwise the API default stands.
            val count = if (arguments.has("count")) arguments.optInt("count", 0) else 0
            if (count > 0) put("count", count)
        }
        val response = httpLayer.post(
            url = "$baseUrl/search",
            headers = mapOf("Authorization" to "Bearer $apiKey"),
            body = body.toString(),
            timeoutMs = 20_000,
        )
        when (response.statusCode) {
            401, 403 -> throw McpAuthException("KieroLabs rejected the API key (HTTP ${response.statusCode})")
        }
        if (response.statusCode !in 200..299) {
            throw McpTransportException("KieroLabs search failed with HTTP ${response.statusCode}: ${response.body.take(200)}")
        }
        return McpToolCallOutcome(isError = false, text = formatResults(response.body))
    }

    /**
     * The API answers JSON. A proxy or error page can answer HTML instead, and
     * showing that raw to the model is worse than saying so plainly.
     */
    private fun formatResults(rawBody: String): String {
        val parsed = runCatching { JSONObject(rawBody) }.getOrNull()
            ?: return "KieroLabs returned a non-JSON response: ${rawBody.take(300)}"
        val results = parsed.optJSONArray("results") ?: JSONArray()
        if (results.length() == 0) return "No results for \"${parsed.optString("query")}\"."
        return buildString {
            append("Results for \"").append(parsed.optString("query")).append("\":\n")
            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                append(i + 1).append(". ").append(item.optString("title")).append('\n')
                append(item.optString("url")).append('\n')
                val snippet = item.optString("snippet")
                if (snippet.isNotBlank()) append(snippet).append('\n')
                append('\n')
            }
        }.trimEnd()
    }
}
