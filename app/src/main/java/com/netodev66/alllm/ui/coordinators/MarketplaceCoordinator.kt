package com.netodev66.alllm.ui.coordinators

import com.netodev66.alllm.marketplace.CURATED_PRESEED_IDS
import com.netodev66.alllm.marketplace.CURATED_SEED_VERSION
import com.netodev66.alllm.marketplace.DeveloperMarketplaceCatalog
import com.netodev66.alllm.marketplace.MarketplaceCategory
import com.netodev66.alllm.marketplace.MarketplaceInstallManager
import com.netodev66.alllm.marketplace.MarketplaceInstallResult
import com.netodev66.alllm.marketplace.MarketplaceItem
import com.netodev66.alllm.marketplace.MarketplacePublishedItemsStore
import com.netodev66.alllm.marketplace.fetchBranchHeadSha
import com.netodev66.alllm.marketplace.fetchRepoDefaultBranch
import com.netodev66.alllm.marketplace.isUpdateAvailable
import com.netodev66.alllm.marketplace.parseGitHubRepo
import com.netodev66.alllm.marketplace.pendingSeedIds
import com.netodev66.alllm.marketplace.resolveUpstreamHeadSha
import com.netodev66.alllm.marketplace.updateCheckDue
import com.netodev66.alllm.marketplace.validateMarketplaceItem
import com.netodev66.alllm.mcp.McpRegistry
import com.netodev66.alllm.mcp.McpServerInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Marketplace domain coordinator (Milestone 1).
 *
 * Explicit owner of the marketplace install state machine. All installs go
 * through the real [MarketplaceInstallManager] (durable records + on-disk
 * package materialization); results are honest (`Installed` / `Failed(reason)`
 * / `AlreadyInstalled`), never fake success.
 *
 * It also owns publishing: a published item is validated, persisted in
 * [MarketplacePublishedItemsStore], and added to the catalog — so it
 * survives restart instead of living only in the catalog's in-memory list.
 * Publishing is local-only: no registry is configured in the app, so a
 * published item is saved on this device, not shared (the dialog says so).
 *
 * Extracted from `MainViewModel`; the ViewModel delegates to this coordinator
 * so its public API is unchanged.
 */
class MarketplaceCoordinator(
    private val host: CoordinatorHost,
    private val installManager: MarketplaceInstallManager,
    private val publishedStore: MarketplacePublishedItemsStore,
    private val mcpRegistry: McpRegistry,
    /** Refreshes the MCP server list UI after a marketplace (un)install registers one. */
    private val onMcpServersChanged: () -> Unit = {},
) {
    private val _installedIds = MutableStateFlow(installManager.installedIds())
    val installedIds: StateFlow<Set<String>> = _installedIds.asStateFlow()

    private val _updatesAvailable = MutableStateFlow<Set<String>>(emptySet())
    val updatesAvailable: StateFlow<Set<String>> = _updatesAvailable.asStateFlow()

    init {
        publishedStore.load().forEach { DeveloperMarketplaceCatalog.addCustomItem(it) }
        host.scope.launch(Dispatchers.IO) {
            seedCuratedSkills()
            repairMissingMcpRegistrations()
        }
    }

    /**
     * Registers MCP servers for marketplace items that are recorded as installed but
     * have no server behind them.
     *
     * Before the install path registered a server on the [MarketplaceInstallResult.ReferenceRecorded]
     * branch, an MCP item could be persisted as installed while nothing was runnable —
     * the card read "Installed" and the server list stayed empty. Those records are
     * durable, so the fix alone would not repair an existing install; this pass does,
     * once, on startup.
     */
    private fun repairMissingMcpRegistrations() {
        var repaired = false
        for ((id, _) in installManager.installedPackages.value) {
            val item = DeveloperMarketplaceCatalog.getAllItems().find { it.id == id } ?: continue
            if (item.category != MarketplaceCategory.MCP_SERVERS) continue
            val template = com.netodev66.alllm.mcp.McpBuiltInCatalogTemplates
                .find { "catalog-mcp-${it.id}" == item.id }
            val serverId = template?.id ?: "marketplace-${item.id}"
            if (mcpRegistry.getServer(serverId) != null) continue
            if (registerMarketplaceMcpServer(item).isNotEmpty()) repaired = true
        }
        if (repaired) onMcpServersChanged()
    }

    private fun seedCuratedSkills() {
        if (installManager.curatedSeedVersion() >= CURATED_SEED_VERSION) return
        val items = DeveloperMarketplaceCatalog.getAllItems().filter { it.id in CURATED_PRESEED_IDS }
        var seededAny = false
        for (item in pendingSeedIds(CURATED_PRESEED_IDS, installManager.installedIds()).mapNotNull { id ->
            items.find { it.id == id }
        }) {
            val sha = runCatching { resolveUpstreamHeadSha(item.installUrl) }.getOrNull().orEmpty()
            if (installManager.seedCurated(item, sha)) seededAny = true
        }
        installManager.setCuratedSeedVersion(CURATED_SEED_VERSION)
        if (seededAny) _installedIds.value = installManager.installedIds()
    }

    fun checkMarketplaceUpdates() {
        host.scope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            if (!updateCheckDue(installManager.lastUpdateCheckAt(), now)) return@launch
            val found = mutableSetOf<String>()
            var anySuccess = false
            for ((id, record) in installManager.installedPackages.value) {
                val item = DeveloperMarketplaceCatalog.getAllItems().find { it.id == id } ?: continue
                val repo = parseGitHubRepo(item.installUrl) ?: continue
                val head = runCatching {
                    val branch = fetchRepoDefaultBranch(repo.first, repo.second) ?: return@runCatching null
                    fetchBranchHeadSha(repo.first, repo.second, branch)
                }.getOrNull() ?: continue
                anySuccess = true
                if (isUpdateAvailable(record.upstreamSha, head)) {
                    found.add(id)
                } else if (record.upstreamSha.isBlank()) {
                    installManager.recordUpstreamSha(id, head)
                }
            }
            if (anySuccess) installManager.setLastUpdateCheckAt(now)
            _updatesAvailable.value = found
        }
    }

    fun updateMarketplaceItem(item: MarketplaceItem) {
        host.scope.launch(Dispatchers.IO) {
            val repo = parseGitHubRepo(item.installUrl)
            val outcome = if (repo == null) {
                "Cannot update ${item.name}: no GitHub repo linked"
            } else {
                val head = runCatching {
                    val branch = fetchRepoDefaultBranch(repo.first, repo.second) ?: return@runCatching null
                    fetchBranchHeadSha(repo.first, repo.second, branch)
                }.getOrNull()
                if (head.isNullOrBlank()) {
                    "Could not reach GitHub to update ${item.name}; try again later"
                } else {
                    when (val result = installManager.updateToLatest(item, head)) {
                        is MarketplaceInstallResult.Installed -> {
                            _updatesAvailable.value = _updatesAvailable.value - item.id
                            "Updated ${item.name} to the latest version"
                        }
                        is MarketplaceInstallResult.Failed -> "Update failed: ${result.reason}"
                        else -> "Update finished for ${item.name}"
                    }
                }
            }
            _installedIds.value = installManager.installedIds()
            withContext(Dispatchers.Main) {
                host.updateState { it.copy(toastMessage = outcome) }
            }
        }
    }

    /**
     * Publishes [item]: validates it, persists it durably, and adds it to
     * the catalog. Synchronous on purpose: everything here is in-memory
     * plus a SharedPreferences apply() (async disk write), so the screen's
     * recompose after this call already sees the new item.
     */
    fun publishMarketplaceItem(item: MarketplaceItem) {
        val validation = validateMarketplaceItem(item)
        val outcome = if (!validation.isValid) {
            "Could not publish: ${validation.errors.joinToString("; ")}"
        } else {
            publishedStore.add(item)
            DeveloperMarketplaceCatalog.addCustomItem(item)
            "Saved ${item.name} on this device"
        }
        host.updateState { it.copy(toastMessage = outcome) }
    }

    fun installMarketplaceItem(item: MarketplaceItem) {
        host.scope.launch(Dispatchers.IO) {
            val outcome = when (val result = installManager.install(item)) {
                is MarketplaceInstallResult.Installed -> {
                    val mcpNote = registerMarketplaceMcpServer(item)
                    "Installed ${item.name}$mcpNote"
                }
                is MarketplaceInstallResult.ReferenceRecorded -> {
                    // An MCP listing's install value is the server *command*
                    // ("npx -y …"), not a downloadable artifact, so it always
                    // lands here. Registering the server is the whole point of
                    // the install, so it must happen on this branch too —
                    // otherwise the card reads "Installed" while no server exists.
                    val mcpNote = registerMarketplaceMcpServer(item)
                    if (mcpNote.isEmpty()) {
                        "Saved ${item.name} as a reference — no package was downloaded"
                    } else {
                        "Installed ${item.name}$mcpNote"
                    }
                }
                is MarketplaceInstallResult.Failed ->
                    "Install failed: ${result.reason}"
                MarketplaceInstallResult.AlreadyInstalled ->
                    "${item.name} is already installed"
            }
            _installedIds.value = installManager.installedIds()
            withContext(Dispatchers.Main) {
                host.updateState { it.copy(toastMessage = outcome) }
            }
        }
    }

    /**
     * Registers an installed MCP marketplace item as a usable MCP server.
     * Returns the toast suffix describing what happened; "" means there was
     * nothing to register (non-MCP item, or an install value that is not a
     * command).
     */
    private fun registerMarketplaceMcpServer(item: MarketplaceItem): String {
        if (item.category != MarketplaceCategory.MCP_SERVERS) return ""
        val template = com.netodev66.alllm.mcp.McpBuiltInCatalogTemplates
            .find { "catalog-mcp-${it.id}" == item.id }
        // A catalog item registers under the template's own id, so the MCP
        // catalog badge, "Installed Servers" list and template installs all
        // agree on one identity per logical server.
        val serverId = template?.id ?: "marketplace-${item.id}"
        val server = marketplaceItemToMcpServer(item, serverId)?.copy(
            secretEnvNames = template?.requiredEnv ?: emptyList()
        )
            ?: return ""
        mcpRegistry.installServer(server)
        onMcpServersChanged()
        return " (MCP server registered)"
    }

    fun uninstallMarketplaceItem(item: MarketplaceItem) {
        host.scope.launch(Dispatchers.IO) {
            val removed = installManager.uninstall(item.id)
            if (item.category == MarketplaceCategory.MCP_SERVERS) {
                // Symmetric with install: an uninstalled MCP item must not
                // leave a registered server behind. The server id mirrors
                // registerMarketplaceMcpServer exactly.
                val template = com.netodev66.alllm.mcp.McpBuiltInCatalogTemplates
                    .find { "catalog-mcp-${it.id}" == item.id }
                val serverId = template?.id ?: "marketplace-${item.id}"
                if (mcpRegistry.removeServer(serverId)) {
                    onMcpServersChanged()
                }
            }
            _installedIds.value = installManager.installedIds()
            val outcome = if (removed) "Uninstalled ${item.name}" else "${item.name} was not installed"
            withContext(Dispatchers.Main) {
                host.updateState { it.copy(toastMessage = outcome) }
            }
        }
    }
}

/**
 * Maps a marketplace MCP item to the [McpServerInfo] installing it
 * registers. Returns null when the item's install value cannot become a
 * server command (blank, or an http(s) URL — a URL is a download, not a
 * command to run). [serverId] is supplied by the caller so the registered
 * id stays in step with what the MCP catalog badge and uninstall path use.
 * Pure function, unit-tested.
 */
internal fun marketplaceItemToMcpServer(item: MarketplaceItem, serverId: String): McpServerInfo? {
    val commandLine = item.installUrl.trim()
    if (commandLine.isBlank()) return null
    if (commandLine.startsWith("http://") || commandLine.startsWith("https://")) return null
    // Shared with the template install path, so a listing and the template it
    // derives from always produce the same executable/args pair.
    val (executable, args) = com.netodev66.alllm.mcp.splitMcpCommandLine(commandLine)
    if (executable.isBlank()) return null
    return McpServerInfo(
        id = serverId,
        name = item.name,
        description = item.description,
        command = executable,
        args = args,
        enabled = true,
        isBuiltIn = false,
        projectId = null,
    )
}
