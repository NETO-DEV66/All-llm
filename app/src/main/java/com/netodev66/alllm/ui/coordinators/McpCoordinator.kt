package com.netodev66.alllm.ui.coordinators

import com.netodev66.alllm.mcp.McpAuditLogger
import com.netodev66.alllm.mcp.McpCredentialStore
import com.netodev66.alllm.mcp.McpDeniedException
import com.netodev66.alllm.mcp.McpPermissionManager
import com.netodev66.alllm.mcp.McpPromptRequiredException
import com.netodev66.alllm.mcp.McpRegistry
import com.netodev66.alllm.mcp.McpServerInfo
import com.netodev66.alllm.mcp.McpToolRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** How the user approved a single MCP tool run from the UI. */
enum class McpToolApproval {
    ONE_TIME,
    ALWAYS_ALLOW
}

/**
 * MCP (Model Context Protocol) domain coordinator (Milestone 1).
 *
 * Explicit owner of the MCP server state machine: install/toggle/remove,
 * tool discovery, tool execution and the audit log. Discovery and calls go
 * through [McpToolRegistry] (correct NDJSON wire protocol, permission
 * gating, audit logging, caching, remote support, credential resolution);
 * the registry owns permission evaluation and audit entries, so this
 * coordinator only maps outcomes into UI state and the
 * [com.netodev66.alllm.runtime.McpToolCallResult] shape the callers use.
 *
 * Extracted from `MainViewModel`; the ViewModel delegates to this coordinator
 * so its public API is unchanged.
 */
class McpCoordinator(
    private val host: CoordinatorHost,
    private val mcpRegistry: McpRegistry,
    private val mcpAuditLogger: McpAuditLogger,
    private val mcpPermissionManager: McpPermissionManager,
    private val toolRegistry: McpToolRegistry,
    private val credentialStore: McpCredentialStore,
) {
    fun refreshMcpServers(projectId: String? = host.state().activeProject?.id) {
        val servers = mcpRegistry.listServers(projectId)
        val catalog = mcpRegistry.catalogTemplates
        host.updateState { it.copy(mcpServers = servers, mcpCatalog = catalog) }
    }

    /**
     * Installs a custom MCP server. [secrets] are stored in the credential
     * store (encrypted), never in the server config: [McpServerInfo] only
     * carries their names in `secretEnvNames`. The server is registered
     * first so a failure there never leaves orphaned secrets behind.
     *
     * Every outcome is reported: an install that silently replaced an existing
     * entry is exactly what made "it says installed but nothing happened" hard
     * to tell apart from a real failure.
     */
    fun installMcpServer(server: McpServerInfo, secrets: Map<String, String> = emptyMap()) {
        val existed = mcpRegistry.getServer(server.id) != null
        mcpRegistry.installServer(server)
        secrets.forEach { (name, value) ->
            if (name.isNotBlank() && value.isNotBlank()) {
                credentialStore.setCredential(server.id, name, value)
            }
        }
        refreshMcpServers()
        host.updateState {
            it.copy(
                toastMessage = if (existed) {
                    "Updated MCP server ${server.name}"
                } else {
                    "Installed MCP server ${server.name} (${server.command})"
                },
            )
        }
        // A custom command can be a typo just as easily as a catalog row can name a
        // missing package; both look identical until something tries to start it.
        verifyMcpServer(server.id, server.name)
    }

    fun installMcpTemplate(templateId: String, envOverrides: Map<String, String> = emptyMap(), projectId: String? = host.state().activeProject?.id) {
        val name = mcpRegistry.catalogTemplates.find { it.id == templateId }?.name ?: templateId
        val installedId = runCatching {
            val server = mcpRegistry.installFromTemplate(templateId, envOverrides, projectId)
            refreshMcpServers(projectId)
            server.id
        }.onSuccess {
            host.updateState { it.copy(toastMessage = "Installed MCP server $name") }
        }.onFailure { err ->
            host.updateState { it.copy(toastMessage = "Failed to install MCP template: ${err.message}") }
        }.getOrNull()
        // Verify against the id the registry actually stored: a project-scoped
        // install is keyed "<template>::<project>", not the bare template id.
        if (installedId != null) verifyMcpServer(installedId, name)
    }

    /**
     * Starts a freshly installed server for real and reports whether it came up.
     *
     * Registering a server never proved it could run: a catalog entry whose npm
     * package does not exist registered fine, showed "Installed", and then died
     * on the first tool call with an error the user could not see. This runs the
     * actual handshake at install time and surfaces the server's own error text,
     * so "installed" means installed *and* startable.
     */
    private fun verifyMcpServer(serverId: String, displayName: String) {
        host.scope.launch {
            host.updateState { it.copy(mcpBusy = true, mcpSelectedServerId = serverId, mcpMessage = "Starting $displayName to check it runs...") }
            val probe = withContext(Dispatchers.IO) {
                runCatching { toolRegistry.probe(serverId, host.state().activeProject?.id) }
            }
            probe.onSuccess { result ->
                if (result.success) {
                    host.updateState {
                        it.copy(
                            mcpBusy = false,
                            mcpMessage = "$displayName is ready (${result.toolCount} tools)",
                            toastMessage = "Installed MCP server $displayName - ${result.toolCount} tools available",
                        )
                    }
                } else {
                    // Keep the server installed: the user may fix a credential or
                    // a command and retry. Only the claim that it works is dropped.
                    host.updateState {
                        it.copy(
                            mcpBusy = false,
                            mcpMessage = "$displayName installed but did not start: ${result.detail}",
                            toastMessage = "$displayName installed, but it could not start: ${result.detail}",
                        )
                    }
                }
            }.onFailure { err ->
                host.updateState {
                    it.copy(
                        mcpBusy = false,
                        mcpMessage = "$displayName installed but could not be checked: ${err.message}",
                        toastMessage = "$displayName installed, but it could not be checked: ${err.message}",
                    )
                }
            }
        }
    }

    fun toggleMcpServer(serverId: String, enabled: Boolean) {
        mcpRegistry.toggleServer(serverId, enabled)
        refreshMcpServers()
    }

    fun removeMcpServer(serverId: String) {
        toolRegistry.stopServer(serverId)
        mcpRegistry.removeServerAndCredentials(serverId, credentialStore)
        refreshMcpServers()
    }

    fun updateMcpConfig(server: McpServerInfo) {
        mcpRegistry.updateServer(server)
        refreshMcpServers()
    }

    fun discoverMcpTools(server: McpServerInfo) {
        host.scope.launch {
            host.updateState { it.copy(mcpBusy = true, mcpSelectedServerId = server.id, mcpMessage = "Discovering tools for ${server.name}...") }
            // Registry discovery does blocking process I/O: keep it off Main.
            val res = withContext(Dispatchers.IO) {
                runCatching { toolRegistry.discover(server.id, host.state().activeProject?.id) }
            }
            res.onSuccess { discovered ->
                val tools = discovered.map { tool ->
                    com.netodev66.alllm.runtime.McpToolDefinition(
                        name = tool.name,
                        description = tool.description,
                        inputSchemaJson = tool.inputSchema.toString(),
                        serverId = server.id,
                    )
                }
                host.updateState { it.copy(mcpBusy = false, mcpTools = tools, mcpMessage = "Found ${tools.size} tools") }
            }.onFailure { err ->
                host.updateState { it.copy(mcpBusy = false, mcpMessage = "Tool discovery failed: ${err.message}") }
            }
        }
    }

    suspend fun executeMcpTool(
        server: McpServerInfo,
        toolName: String,
        arguments: JSONObject,
        projectId: String? = host.state().activeProject?.id
    ): com.netodev66.alllm.runtime.McpToolCallResult {
        // Permission evaluation + audit logging are owned by the registry;
        // this coordinator only maps the outcome into the UI-facing shape.
        val startedAt = System.currentTimeMillis()
        val duration = { System.currentTimeMillis() - startedAt }
        return try {
            val outcome = withContext(Dispatchers.IO) {
                toolRegistry.callTool(server.id, toolName, arguments, projectId)
            }
            loadMcpAuditLogs()
            com.netodev66.alllm.runtime.McpToolCallResult(
                success = !outcome.isError,
                content = outcome.text,
                error = if (outcome.isError) outcome.text else null,
                durationMs = duration(),
            )
        } catch (e: McpDeniedException) {
            loadMcpAuditLogs()
            com.netodev66.alllm.runtime.McpToolCallResult(
                success = false,
                content = "",
                error = "Permission check failed: ${e.decision.reason}",
                durationMs = duration(),
            )
        } catch (e: McpPromptRequiredException) {
            loadMcpAuditLogs()
            com.netodev66.alllm.runtime.McpToolCallResult(
                success = false,
                content = "",
                error = "MCP tool requires confirmation: ${e.decision.reason}",
                durationMs = duration(),
            )
        } catch (e: Exception) {
            loadMcpAuditLogs()
            com.netodev66.alllm.runtime.McpToolCallResult(
                success = false,
                content = "",
                error = e.message ?: "MCP tool call failed",
                durationMs = duration(),
            )
        }
    }

    fun setMcpProjectPermission(projectId: String, serverId: String, policy: com.netodev66.alllm.mcp.McpPermissionPolicy) {
        mcpPermissionManager.setProjectServerPolicy(projectId, serverId, policy)
    }

    fun setMcpGlobalPermission(serverId: String, policy: com.netodev66.alllm.mcp.McpPermissionPolicy) {
        mcpPermissionManager.setGlobalServerPolicy(serverId, policy)
    }

    fun mcpToolDecision(
        serverId: String,
        toolName: String,
        projectId: String? = host.state().activeProject?.id
    ): com.netodev66.alllm.mcp.McpPermissionDecision {
        val server = mcpRegistry.getServer(serverId)
            ?: return com.netodev66.alllm.mcp.McpPermissionDecision(
                allowed = false,
                requiresPrompt = false,
                reason = "MCP server '$serverId' is not installed"
            )
        return mcpPermissionManager.evaluate(server, toolName, projectId)
    }

    fun grantOneTimeMcpToolApproval(
        serverId: String,
        toolName: String,
        projectId: String? = host.state().activeProject?.id
    ) {
        mcpPermissionManager.grantOneTimeApproval(serverId, toolName, projectId)
    }

    fun allowMcpToolAlways(
        serverId: String,
        toolName: String,
        projectId: String? = host.state().activeProject?.id
    ) {
        if (projectId != null) {
            mcpPermissionManager.setProjectToolPolicy(
                projectId, serverId, toolName,
                com.netodev66.alllm.mcp.McpPermissionPolicy.ALLOW_ALWAYS
            )
        } else {
            mcpPermissionManager.setGlobalToolPolicy(
                serverId, toolName,
                com.netodev66.alllm.mcp.McpPermissionPolicy.ALLOW_ALWAYS
            )
        }
    }

    fun loadMcpAuditLogs(limit: Int = 100) {
        val logs = mcpAuditLogger.getEntries(limit)
        host.updateState { it.copy(mcpAuditLogs = logs) }
    }

    fun clearMcpAuditLogs() {
        mcpAuditLogger.clear()
        host.updateState { it.copy(mcpAuditLogs = emptyList()) }
    }
}
