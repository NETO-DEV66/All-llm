package com.netodev66.alllm.ui

import com.netodev66.alllm.model.ActivityItem
import com.netodev66.alllm.model.ActiveRoadmap
import com.netodev66.alllm.model.ChangeItem
import com.netodev66.alllm.model.ChatMessage
import com.netodev66.alllm.model.ProjectChat
import com.netodev66.alllm.model.ScratchpadItem
import com.netodev66.alllm.model.ToolRequest
import com.netodev66.alllm.session.EngineeringState
import com.netodev66.alllm.session.PersistedAgentSession

data class OrchestratorTransition(
    val state: EngineeringState,
    val stepTitle: String,
    val details: String? = null,
)

data class ProjectSessionState(
    val projectId: String,
    val messages: List<ChatMessage> = emptyList(),
    val projectChats: List<ProjectChat> = emptyList(),
    val activeChatId: String? = null,
    val pendingApproval: ToolRequest? = null,
    val changes: List<ChangeItem> = emptyList(),
    val activity: List<ActivityItem> = emptyList(),
    val liveProcess: List<ActivityItem> = emptyList(),
    val liveThinking: Boolean = false,
    val activeThinkingBlockId: Long? = null,
    val taskStartedAtMillis: Long? = null,
    val taskFinishedAtMillis: Long? = null,
    val workSegmentStartedAtMillis: Long? = null,
    val currentTaskRequest: String? = null,
    val previewReady: Boolean = false,
    val previewUrl: String? = null,
    val isRunning: Boolean = false,
    val streamingMessageId: String? = null,
    val activeSessionId: String? = null,
    val interruptedSession: PersistedAgentSession? = null,
    val runtimeRecoveryStatus: String? = null,
    val runtimeRecoveryAttempt: Int = 0,
    val runtimeRecoveryCanResume: Boolean = false,
    val activeRoadmap: ActiveRoadmap? = null,
    val scratchpadItems: List<ScratchpadItem> = emptyList(),
    val pendingOrchestratorTransition: OrchestratorTransition? = null,
)
