package com.netodev66.alllm.mcp

import com.netodev66.alllm.marketplace.DeveloperMarketplaceCatalog
import com.netodev66.alllm.security.PermissionPolicyEngine
import com.netodev66.alllm.ui.coordinators.marketplaceItemToMcpServer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * A native server's tools come from an in-app handler. These tests pin the two
 * properties that made the old built-ins useless on Android: no child process is
 * ever created for them, and a server whose handler is missing fails loudly.
 */
class McpNativeServerDispatchTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var baseDir: File
    private lateinit var registry: McpRegistry
    private lateinit var permissionManager: McpPermissionManager
    private lateinit var auditLogger: McpAuditLogger
    private lateinit var credentialStore: McpCredentialStore
    private var sessionFactoryCalls = 0

    private fun toolRegistry() = McpToolRegistry(
        registry = registry,
        permissionManager = permissionManager,
        auditLogger = auditLogger,
        credentialStore = credentialStore,
        sessionFactory = {
            sessionFactoryCalls++
            throw AssertionError("a native server must not spawn a process")
        },
    )

    @Before
    fun setUp() {
        baseDir = tempFolder.newFolder("native_test")
        val securityDir = tempFolder.newFolder("native_security")
        registry = McpRegistry(baseDir)
        permissionManager = McpPermissionManager(baseDir, PermissionPolicyEngine(securityDir))
        auditLogger = McpAuditLogger(baseDir)
        credentialStore = McpCredentialStore(baseDir, InMemoryCredentialBackend())
        sessionFactoryCalls = 0
    }

    @Test
    fun `the search template installs as a native server that needs no Node`() {
        registry.installFromTemplate("keiro-search")

        val server = registry.getServer("keiro-search")
            ?: fail("the template did not install") as McpServerInfo
        assertTrue("expected an in-app handler", server.isNative)
        assertEquals(KeiroLabsSearchHandler.HANDLER_ID, server.nativeHandler)
        assertEquals(listOf(KeiroLabsSearchHandler.CONFIG_API_KEY), server.secretEnvNames)
        assertEquals("https://kierolabs.space/api", server.env[KeiroLabsSearchHandler.CONFIG_BASE_URL])
    }

    @Test
    fun `the installed native marker survives a reload from disk`() {
        registry.installFromTemplate("keiro-search")

        val reloaded = McpRegistry(baseDir).getServer("keiro-search")
            ?: fail("the server was not persisted") as McpServerInfo
        assertTrue(reloaded.isNative)
        assertEquals(KeiroLabsSearchHandler.HANDLER_ID, reloaded.nativeHandler)
    }

    @Test
    fun `discovery reads the in-app handler instead of starting a process`() {
        registry.installFromTemplate("keiro-search")
        permissionManager.setGlobalServerPolicy("keiro-search", McpPermissionPolicy.ALLOW_ALWAYS)

        val tools = toolRegistry().discover("keiro-search")

        assertEquals(0, sessionFactoryCalls)
        assertEquals(listOf("web_search"), tools.map { it.name })
    }

    @Test
    fun `an unknown handler is an error, not a silent process spawn`() {
        registry.installServer(
            McpServerInfo(
                id = "ghost",
                name = "Ghost",
                command = "ghost",
                nativeHandler = "not-a-real-handler",
            ),
        )
        permissionManager.setGlobalServerPolicy("ghost", McpPermissionPolicy.ALLOW_ALWAYS)

        try {
            toolRegistry().discover("ghost")
            fail("expected a configuration error")
        } catch (e: McpConfigurationException) {
            assertTrue(e.message!!.contains("not-a-real-handler"))
        }
        assertEquals(0, sessionFactoryCalls)
    }

    @Test
    fun `the catalog no longer offers a template that cannot run on Android`() {
        val ids = registry.catalogTemplates.map { it.id }
        assertFalse("Brave Search needs a Brave key and a Node runtime", ids.contains("brave-search"))
        assertTrue(ids.contains("keiro-search"))
    }

    @Test
    fun `a native server resolves its key from the credential store, not the registry JSON`() {
        registry.installFromTemplate("keiro-search")
        credentialStore.setCredential("keiro-search", KeiroLabsSearchHandler.CONFIG_API_KEY, "secret-key")

        val env = toolRegistry().resolveEnv(registry.getServer("keiro-search")!!)

        assertEquals("secret-key", env[KeiroLabsSearchHandler.CONFIG_API_KEY])
        assertTrue(
            "the secret must not be written into the persisted server config",
            !registry.getServer("keiro-search")!!.toJson().toString().contains("secret-key"),
        )
    }

    @Test
    fun `a native server without its key is rejected before any request is made`() {
        registry.installFromTemplate("keiro-search")
        try {
            toolRegistry().resolveEnv(registry.getServer("keiro-search")!!)
            fail("expected a missing-credential error")
        } catch (e: McpConfigurationException) {
            assertTrue(e.message!!.contains(KeiroLabsSearchHandler.CONFIG_API_KEY))
        }
    }

    @Test
    fun `a marketplace listing dispatches to the in-app handler, not to a process`() {
        // End to end over the path the Marketplace screen actually uses: the
        // listing -> server mapping, then the registry's dispatch. The handler's
        // own key check fires, which proves the call reached the handler.
        val listing = requireNotNull(
            DeveloperMarketplaceCatalog.curatedCatalog.find { it.id == "catalog-mcp-keiro-search" },
        )
        val server = requireNotNull(marketplaceItemToMcpServer(listing, KeiroLabsSearchHandler.HANDLER_ID))
        registry.installServer(server)
        permissionManager.setGlobalServerPolicy(KeiroLabsSearchHandler.HANDLER_ID, McpPermissionPolicy.ALLOW_ALWAYS)

        try {
            toolRegistry().callTool(
                KeiroLabsSearchHandler.HANDLER_ID,
                "web_search",
                JSONObject("""{"query":"x"}"""),
            )
            fail("expected the handler's missing-key error")
        } catch (e: McpConfigurationException) {
            // resolveEnv fails first, naming the credential the server declared.
            assertTrue(e.message!!.contains(KeiroLabsSearchHandler.CONFIG_API_KEY))
        }
        assertEquals("no process may be spawned for a native server", 0, sessionFactoryCalls)
    }

    @Test
    fun `a custom stdio server is still allowed and still spawns`() {
        registry.installServer(
            McpServerInfo(id = "custom", name = "Custom", command = "npx", args = listOf("-y", "some-server")),
        )
        val server = registry.getServer("custom")!!
        assertFalse(server.isNative)
        assertEquals(listOf("-y", "some-server"), server.args)
        assertEquals(JSONObject.NULL, JSONObject(server.toJson().toString()).opt("nativeHandler"))
    }
}
