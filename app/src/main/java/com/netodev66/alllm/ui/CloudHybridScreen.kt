@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.netodev66.alllm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netodev66.alllm.agent.team.model.TeamTask
import com.netodev66.alllm.agent.team.model.TeamTaskStatus
import com.netodev66.alllm.runtime.DeviceTelemetry
import com.netodev66.alllm.runtime.DeviceTelemetryFormat
import com.netodev66.alllm.runtime.DeviceTelemetryReader
import com.netodev66.alllm.ui.theme.BouncyAppear
import com.netodev66.alllm.ui.theme.GlassPanel
import com.netodev66.alllm.ui.theme.GlassTopAppBar
import com.netodev66.alllm.ui.theme.PocketIconButton
import com.netodev66.alllm.ui.theme.bouncyEntry
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Cloud Hybrid: what this phone can contribute to a hybrid run, plus the live
 * jobs that are actually running.
 *
 * This screen used to be a five-page onboarding followed by an immediate
 * `closeOverlay()` — it promised a workers fleet, routing policies and a job
 * pipeline that do not exist anywhere in the codebase, then silently vanished.
 * Rather than fake those panels, this shows only what the app really has:
 * measured device telemetry, the real concurrency ceiling the scheduler will
 * enforce, and the team tasks currently executing. Anything not yet built is
 * labelled as such instead of being drawn as a working control.
 */
@Composable
fun CloudHybridScreen(
    tasks: List<TeamTask>,
    isRunning: Boolean,
    maxConcurrentAgents: Int,
    throttleReason: String?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val reader = remember(context) { DeviceTelemetryReader(context) }
    var telemetry by remember { mutableStateOf<DeviceTelemetry?>(null) }

    // Re-read while the screen is open so the numbers track the real device.
    LaunchedEffect(Unit) {
        while (true) {
            telemetry = runCatching { reader.read() }.getOrNull()
            delay(2_000)
        }
    }

    Scaffold(
        topBar = {
            GlassTopAppBar(
                title = { Text("Cloud Hybrid", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    PocketIconButton(onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .bouncyEntry(key = Unit, fromScale = 0.985f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { HeaderCard(isRunning) }
            item { TelemetryPanel(telemetry) }
            item { SchedulerPanel(maxConcurrentAgents, throttleReason, isRunning) }
            item {
                JobPipelinePanel(
                    tasks = tasks,
                    isRunning = isRunning,
                )
            }
            item { NotBuiltYetPanel() }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun HeaderCard(isRunning: Boolean) {
    BouncyAppear {
        GlassPanel(strong = true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Hybrid execution", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        if (isRunning) "A team run is using this device now" else "This device is ready to take work",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                // Honest scope statement: the "cloud" half of Cloud Hybrid is the
                // remote agents you configure in Agent settings, not a bundled fleet.
                "Heavy jobs can be handed to a remote agent you have configured; everything else runs " +
                    "on this phone. This panel reports what the device can really take on.",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TelemetryPanel(telemetry: DeviceTelemetry?) {
    GlassPanel(title = "DEVICE TELEMETRY") {
        if (telemetry == null) {
            Text(
                "Reading device sensors…",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@GlassPanel
        }
        TelemetryRow(
            Icons.Default.BatteryFull,
            "Battery",
            when {
                telemetry.batteryPercent == null -> "Unavailable"
                telemetry.batteryCharging == true -> "${telemetry.batteryPercent}% · charging"
                else -> "${telemetry.batteryPercent}%"
            },
            telemetry.batteryPercent != null && telemetry.batteryPercent in 0..14 && telemetry.batteryCharging != true,
        )
        TelemetryRow(
            Icons.Default.Speed,
            "Thermal",
            telemetry.thermalLabel ?: "Not reported below Android 10",
            DeviceTelemetryFormat.isThrottling(telemetry.thermalLevel),
        )
        TelemetryRow(
            Icons.Default.Memory,
            "Memory free",
            freeMemoryLabel(telemetry),
            DeviceTelemetryFormat.freeRamRatio(telemetry.availableRamBytes, telemetry.totalRamBytes)
                ?.let { it < 0.15f } == true,
        )
        TelemetryRow(
            Icons.Default.Devices,
            "CPU cores",
            telemetry.cpuCores?.toString() ?: "Unavailable",
            false,
        )
        TelemetryRow(
            Icons.Default.Wifi,
            "Network",
            when {
                telemetry.networkLabel == null -> "Offline"
                telemetry.metered == true -> "${telemetry.networkLabel} · metered"
                else -> "${telemetry.networkLabel} · unmetered"
            },
            telemetry.networkLabel == null,
        )
    }
}

@Composable
private fun freeMemoryLabel(telemetry: DeviceTelemetry): String {
    val available = telemetry.availableRamBytes ?: return "Unavailable"
    val total = telemetry.totalRamBytes
    val free = formatGigabytes(available)
    return if (total != null && total > 0L) {
        val ratio = DeviceTelemetryFormat.freeRamRatio(available, total) ?: 0f
        "$free free of ${formatGigabytes(total)} (${(ratio * 100).toInt()}%)"
    } else {
        "$free free"
    }
}

private fun formatGigabytes(bytes: Long): String =
    String.format(Locale.US, "%.1f GB", bytes.toDouble() / 1_073_741_824.0)

@Composable
private fun SchedulerPanel(maxConcurrentAgents: Int, throttleReason: String?, isRunning: Boolean) {
    GlassPanel(title = "LOCAL CONCURRENCY") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$maxConcurrentAgents",
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (maxConcurrentAgents == 1) "agent at a time" else "agents at a time",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Ceiling enforced by the adaptive scheduler on this hardware.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (throttleReason != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Currently reduced: $throttleReason.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error,
            )
        } else if (isRunning) {
            Spacer(Modifier.height(10.dp))
            Text(
                "No pressure detected; running at full local capacity.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun JobPipelinePanel(tasks: List<TeamTask>, isRunning: Boolean) {
    GlassPanel(title = "JOB PIPELINE") {
        if (tasks.isEmpty()) {
            Text(
                if (isRunning) {
                    "The team is starting up; tasks will appear here as they are planned."
                } else {
                    "No jobs yet. Start a team run in Multi-Agent Studio and its tasks will be listed here."
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@GlassPanel
        }
        val active = tasks.filter { it.status == TeamTaskStatus.IN_PROGRESS }.size
        val done = tasks.filter { it.status == TeamTaskStatus.COMPLETED }.size
        val failed = tasks.filter { it.status == TeamTaskStatus.FAILED }.size
        Text(
            "$active running · $done completed${if (failed > 0) " · $failed failed" else ""} of ${tasks.size}",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        tasks.take(8).forEach { task ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(task.status.indicatorColor()),
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    task.title,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                Text(
                    task.status.label(),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (tasks.size > 8) {
            Spacer(Modifier.height(4.dp))
            Text(
                "+${tasks.size - 8} more in Multi-Agent Studio",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NotBuiltYetPanel() {
    GlassPanel(title = "NOT BUILT YET") {
        Text(
            "Remote worker management and routing policies are not implemented. The earlier " +
                "walkthrough described them as if they were, which was wrong. When a real remote " +
                "runner exists it will be configured here.",
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TelemetryRow(
    icon: ImageVector,
    label: String,
    value: String,
    warn: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(
            value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (warn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun TeamTaskStatus.indicatorColor(): Color = when (this) {
    TeamTaskStatus.IN_PROGRESS -> Color(0xFF58C9A3)
    TeamTaskStatus.COMPLETED -> Color(0xFF69D69E)
    TeamTaskStatus.FAILED -> Color(0xFFE5484D)
    TeamTaskStatus.BLOCKED -> Color(0xFFF2A03D)
    TeamTaskStatus.UNDER_REVIEW -> Color(0xFF8EA8FF)
    TeamTaskStatus.READY -> Color(0xFF8EA8FF)
    TeamTaskStatus.PENDING -> Color(0xFF8B949E)
}

private fun TeamTaskStatus.label(): String = when (this) {
    TeamTaskStatus.PENDING -> "queued"
    TeamTaskStatus.READY -> "ready"
    TeamTaskStatus.IN_PROGRESS -> "running"
    TeamTaskStatus.BLOCKED -> "blocked"
    TeamTaskStatus.UNDER_REVIEW -> "in review"
    TeamTaskStatus.COMPLETED -> "done"
    TeamTaskStatus.FAILED -> "failed"
}
