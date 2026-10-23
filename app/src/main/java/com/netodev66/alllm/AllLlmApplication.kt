package com.netodev66.alllm

import android.app.Application

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.netodev66.alllm.runtime.KeepAliveService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Process-wide bootstrap. Installed before activities/services so fatal Java/Kotlin
 * startup exceptions are persisted before Android terminates the process.
 *
 * Crash reporting writes to the on-device log so every uncaught exception is recorded
 * automatically — nothing is uploaded off the device.
 */
class AllLlmApplication : Application() {
    companion object {
        val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    override fun onCreate() {
        super.onCreate()
        AppCrashLogger.initialize(this)
        reportPreviousDeath()
        runCatching { com.netodev66.alllm.runtime.AnrWatchdog.start() }
        runCatching {
            val pm = getSystemService(POWER_SERVICE) as? android.os.PowerManager
            val ignored = pm?.isIgnoringBatteryOptimizations(packageName) ?: false
            AppCrashLogger.log("Battery optimization ignored=$ignored")
        }
        AppCrashLogger.log("Application.onCreate completed")
        // Keep-alive: when the app goes to the background, start a lightweight
        // foreground service so aggressive OEM task killers do not freeze the
        // process. When the app returns to the foreground the service stops,
        // so no persistent notification shows while the app is in use.
        runCatching {
            ProcessLifecycleOwner.get().lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onStop(owner: LifecycleOwner) {
                        markBackgrounded(true)
                        runCatching { KeepAliveService.start(this@AllLlmApplication) }
                    }

                    override fun onStart(owner: LifecycleOwner) {
                        markBackgrounded(false)
                        runCatching { KeepAliveService.stop(this@AllLlmApplication) }
                    }
                }
            )
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        AppCrashLogger.log("onTrimMemory level=$level")
    }

    private fun forensicsPrefs() =
        getSharedPreferences("process_forensics", MODE_PRIVATE)

    private fun reportPreviousDeath() {
        runCatching {
            val prefs = forensicsPrefs()
            val cleanExit = prefs.getBoolean("clean_exit", true)
            val wasBackground = prefs.getBoolean("was_background", false)
            if (!cleanExit) {
                if (wasBackground) {
                    AppCrashLogger.log("PREVIOUS PROCESS DIED IN BACKGROUND (system reclaim, likely normal)")
                } else {
                    AppCrashLogger.log("PREVIOUS PROCESS DIED WHILE FOREGROUND (crash/kill, investigate)")
                }
            }
            prefs.edit().putBoolean("clean_exit", false).apply()
        }
    }

    fun markCleanExit() {
        runCatching {
            forensicsPrefs().edit().putBoolean("clean_exit", true).apply()
        }
    }

    fun markBackgrounded(background: Boolean) {
        runCatching {
            forensicsPrefs().edit().putBoolean("was_background", background).apply()
        }
    }
}
