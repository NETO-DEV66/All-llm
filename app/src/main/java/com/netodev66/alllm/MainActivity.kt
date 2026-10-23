package com.netodev66.alllm

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.netodev66.alllm.ui.MainViewModel
import com.netodev66.alllm.ui.PocketDevApp
import com.netodev66.alllm.ui.theme.GlassBackdrop
import com.netodev66.alllm.ui.theme.PocketTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCrashLogger.log("MainActivity.onCreate entered")
        try {
            super.onCreate(savedInstanceState)
            AppCrashLogger.log("MainActivity.onCreate: super.onCreate completed")

            enableEdgeToEdge()
            AppCrashLogger.log("MainActivity.onCreate: edge-to-edge enabled")

            val notificationRoute = savedInstanceState?.getString(EXTRA_NOTIFICATION_ROUTE)
                ?: intent.getStringExtra(EXTRA_NOTIFICATION_ROUTE)
            AppCrashLogger.log("MainActivity.onCreate: notification route resolved=\${notificationRoute != null}")

            val vm = ViewModelProvider(this)[MainViewModel::class.java]
            AppCrashLogger.log("MainActivity.onCreate: MainViewModel obtained")

            vm.handleNotificationRoute(notificationRoute)
            AppCrashLogger.log("MainActivity.onCreate: notification route handled")

            setContent {
                val state by vm.state.collectAsStateWithLifecycle()
                PocketTheme(themeMode = state.themeMode) {
                    // The animated glass backdrop is the single base layer for the
                    // whole app; every screen's Scaffold is transparent over it, which
                    // is what lets the translucent panels read as real glass.
                    GlassBackdrop {
                        PocketDevApp(vm)
                    }
                }
            }
            AppCrashLogger.log("MainActivity.onCreate completed successfully")
        } catch (throwable: Throwable) {
            AppCrashLogger.logThrowable("MainActivity.onCreate FAILED", throwable)
            throw throwable
        }
    }

    override fun onNewIntent(intent: Intent) {
        AppCrashLogger.log("MainActivity.onNewIntent entered")
        try {
            super.onNewIntent(intent)
            setIntent(intent)
            intent.getStringExtra(EXTRA_NOTIFICATION_ROUTE)?.let { route ->
                val vm = ViewModelProvider(this)[MainViewModel::class.java]
                vm.handleNotificationRoute(route)
            }
            AppCrashLogger.log("MainActivity.onNewIntent completed")
        } catch (throwable: Throwable) {
            AppCrashLogger.logThrowable("MainActivity.onNewIntent FAILED", throwable)
            throw throwable
        }
    }

    override fun onDestroy() {
        runCatching {
            (application as? AllLlmApplication)?.markCleanExit()
        }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_NOTIFICATION_ROUTE = "notification_route"
    }
}
