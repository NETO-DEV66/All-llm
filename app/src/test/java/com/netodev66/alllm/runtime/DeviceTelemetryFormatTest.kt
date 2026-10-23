package com.netodev66.alllm.runtime

import android.os.PowerManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Cloud Hybrid panel renders whatever [DeviceTelemetryFormat] returns, so a
 * wrong mapping here would be shown to the user as a hardware fact. These cover
 * the two cases that matter most: "the platform did not tell us" must stay null
 * rather than becoming a fabricated reading, and the throttle threshold must
 * match what [AdaptiveConcurrencyController] enforces.
 */
class DeviceTelemetryFormatTest {

    @Test
    fun `missing thermal reading stays unknown instead of nominal`() {
        assertNull(DeviceTelemetryFormat.thermalLabel(null))
        assertFalse(DeviceTelemetryFormat.isThrottling(null))
    }

    @Test
    fun `thermal codes map to their labels`() {
        assertEquals("Nominal", DeviceTelemetryFormat.thermalLabel(PowerManager.THERMAL_STATUS_NONE))
        assertEquals("Moderate", DeviceTelemetryFormat.thermalLabel(PowerManager.THERMAL_STATUS_MODERATE))
        assertEquals("Critical", DeviceTelemetryFormat.thermalLabel(PowerManager.THERMAL_STATUS_CRITICAL))
        assertEquals("Unknown", DeviceTelemetryFormat.thermalLabel(999))
    }

    @Test
    fun `throttling starts at moderate, matching the scheduler`() {
        assertFalse(DeviceTelemetryFormat.isThrottling(PowerManager.THERMAL_STATUS_NONE))
        assertFalse(DeviceTelemetryFormat.isThrottling(PowerManager.THERMAL_STATUS_LIGHT))
        assertTrue(DeviceTelemetryFormat.isThrottling(PowerManager.THERMAL_STATUS_MODERATE))
        assertTrue(DeviceTelemetryFormat.isThrottling(PowerManager.THERMAL_STATUS_SHUTDOWN))
    }

    @Test
    fun `free ram ratio needs both readings and clamps`() {
        assertNull(DeviceTelemetryFormat.freeRamRatio(null, 8_000_000_000L))
        assertNull(DeviceTelemetryFormat.freeRamRatio(1_000L, null))
        assertNull(DeviceTelemetryFormat.freeRamRatio(1_000L, 0L))
        assertEquals(0.25f, DeviceTelemetryFormat.freeRamRatio(2_000L, 8_000L)!!, 0.0001f)
        // A gauge glitch reporting more free than total must not produce >1.
        assertEquals(1f, DeviceTelemetryFormat.freeRamRatio(9_000L, 8_000L)!!, 0.0001f)
    }

    @Test
    fun `network label prefers the physical transport`() {
        assertEquals(
            "Ethernet",
            DeviceTelemetryFormat.networkLabel(
                hasWifi = true,
                hasCellular = true,
                hasEthernet = true,
                hasVpn = false,
                hasBluetooth = false,
            ),
        )
        assertEquals(
            "Wi-Fi",
            DeviceTelemetryFormat.networkLabel(
                hasWifi = true,
                hasCellular = false,
                hasEthernet = false,
                hasVpn = false,
                hasBluetooth = false,
            ),
        )
        assertEquals(
            "VPN",
            DeviceTelemetryFormat.networkLabel(
                hasWifi = false,
                hasCellular = true,
                hasEthernet = false,
                hasVpn = true,
                hasBluetooth = false,
            ),
        )
    }

    @Test
    fun `no transport at all reports offline`() {
        assertNull(
            DeviceTelemetryFormat.networkLabel(
                hasWifi = false,
                hasCellular = false,
                hasEthernet = false,
                hasVpn = false,
                hasBluetooth = false,
            ),
        )
    }
}
