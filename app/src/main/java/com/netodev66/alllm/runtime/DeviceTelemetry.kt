package com.netodev66.alllm.runtime

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.netodev66.alllm.AppCrashLogger

/**
 * A point-in-time reading of the hardware the app is running on.
 *
 * Every field is nullable because "the platform would not tell us" is a real
 * answer on stripped ROMs: `getSystemService` can return null, battery capacity
 * can report [Int.MIN_VALUE] while the fuel gauge is still initialising, and
 * thermal status does not exist before API 29. A null is rendered as "unknown",
 * never as a fabricated zero — the same honesty rule the team runtime follows.
 */
data class DeviceTelemetry(
    val batteryPercent: Int? = null,
    val batteryCharging: Boolean? = null,
    val thermalLabel: String? = null,
    val thermalLevel: Int? = null,
    val cpuCores: Int? = null,
    val totalRamBytes: Long? = null,
    val availableRamBytes: Long? = null,
    val lowRamDevice: Boolean? = null,
    val networkLabel: String? = null,
    val metered: Boolean? = null,
)

/**
 * Pure presentation helpers, kept out of the Compose layer so they can be unit
 * tested on the JVM without an Android device.
 */
object DeviceTelemetryFormat {

    /** Thermal status code to a short human label; null when the reading is absent. */
    fun thermalLabel(level: Int?): String? = when (level) {
        null -> null
        PowerManager.THERMAL_STATUS_NONE -> "Nominal"
        PowerManager.THERMAL_STATUS_LIGHT -> "Light"
        PowerManager.THERMAL_STATUS_MODERATE -> "Moderate"
        PowerManager.THERMAL_STATUS_SEVERE -> "Severe"
        PowerManager.THERMAL_STATUS_CRITICAL -> "Critical"
        PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency"
        PowerManager.THERMAL_STATUS_SHUTDOWN -> "Shutdown"
        else -> "Unknown"
    }

    /** True when the thermal reading is high enough that the app should back off. */
    fun isThrottling(level: Int?): Boolean =
        level != null && level >= PowerManager.THERMAL_STATUS_MODERATE

    /** Memory pressure as a 0..1 fraction of total RAM that is still free. */
    fun freeRamRatio(available: Long?, total: Long?): Float? {
        if (available == null || total == null || total <= 0L) return null
        return (available.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Map [NetworkCapabilities] transport/bandwidth flags to a label.
     * Metered is separate from the label because a metered Wi-Fi hotspot is a
     * real case the label alone would hide.
     */
    fun networkLabel(
        hasWifi: Boolean,
        hasCellular: Boolean,
        hasEthernet: Boolean,
        hasVpn: Boolean,
        hasBluetooth: Boolean,
    ): String? = when {
        hasEthernet -> "Ethernet"
        hasVpn -> "VPN"
        hasWifi -> "Wi-Fi"
        hasCellular -> "Cellular"
        hasBluetooth -> "Bluetooth"
        else -> null
    }
}

/**
 * Reads live device telemetry for the Cloud Hybrid screen.
 *
 * This is deliberately a plain reader with no state: the Cloud Hybrid panel is
 * showing what the phone can contribute to a hybrid run, so a stale cached value
 * would be worse than a missing one.
 */
class DeviceTelemetryReader(private val context: Context) {

    fun read(): DeviceTelemetry {
        val battery = readBattery()
        val thermal = readThermal()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        val memoryInfo = activityManager?.let { manager ->
            runCatching { android.app.ActivityManager.MemoryInfo().also(manager::getMemoryInfo) }
                .onFailure { AppCrashLogger.log("Telemetry memory read failed: ${it.message}") }
                .getOrNull()
        }
        val network = readNetwork()

        return DeviceTelemetry(
            batteryPercent = battery?.first,
            batteryCharging = battery?.second,
            thermalLabel = DeviceTelemetryFormat.thermalLabel(thermal),
            thermalLevel = thermal,
            cpuCores = runCatching { Runtime.getRuntime().availableProcessors() }.getOrNull(),
            totalRamBytes = memoryInfo?.totalMem,
            availableRamBytes = memoryInfo?.availMem,
            lowRamDevice = activityManager?.isLowRamDevice,
            networkLabel = network?.label,
            metered = network?.metered,
        )
    }

    /** Battery percent and charging flag, or null when the fuel gauge is unreadable. */
    private fun readBattery(): Pair<Int, Boolean>? {
        val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return null
        val level = runCatching {
            manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        }.getOrNull() ?: return null
        // The property reports a negative sentinel when the gauge is not ready.
        if (level < 0 || level > 100) return null
        val charging = runCatching {
            val status = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                ?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        }.getOrDefault(manager.isCharging)
        return level to charging
    }

    private fun readThermal(): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val manager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return null
        return runCatching { manager.currentThermalStatus }
            .onFailure { AppCrashLogger.log("Telemetry thermal read failed: ${it.message}") }
            .getOrNull()
    }

    private data class NetworkReading(val label: String, val metered: Boolean)

    private fun readNetwork(): NetworkReading? {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
        val capabilities = runCatching {
            manager.getNetworkCapabilities(manager.activeNetwork)
        }.onFailure { AppCrashLogger.log("Telemetry network read failed: ${it.message}") }.getOrNull() ?: return null

        val label = DeviceTelemetryFormat.networkLabel(
            hasWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
            hasCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR),
            hasEthernet = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET),
            hasVpn = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN),
            hasBluetooth = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH),
        ) ?: return null

        // "Not metered" is reported as the presence of the NOT_METERED capability.
        return NetworkReading(label = label, metered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED))
    }
}
