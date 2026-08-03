package com.agnes.bundle_agnes.feature.chat.repository

import com.sobrr.agnes.feature_chat.model.ChatMessage
import com.sobrr.agnes.feature_chat.model.ChatStreamRequestBody
import com.sobrr.agnes.feature_chat.model.StreamBlock
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.StateFlow

interface ChatRepository {
    val messages: StateFlow<List<ChatMessage>>
    val isStreaming: StateFlow<Boolean>

    suspend fun sendMessage(request: ChatStreamRequestBody): Result<ReceiveChannel<StreamBlock>>
    suspend fun cancelStream(conversationId: String): Result<Unit>
    suspend fun resumeStream(conversationId: String, lastEventId: String): Result<ReceiveChannel<StreamBlock>>
    suspend fun regenerateMessage(conversationId: String, messageId: String): Result<ReceiveChannel<StreamBlock>>
    suspend fun hitlResume(conversationId: String, hitlData: String, toolCallId: String): Result<ReceiveChannel<StreamBlock>>

    // Conversation management
    suspend fun getConversations(page: Int, pageSize: Int, keyword: String?): Result<ConversationListResult>
    suspend fun createConversation(title: String?, agentType: String?): Result<String> // returns conversationId
    suspend fun getConversationDetail(conversationId: String): Result<ConversationDetail>
    suspend fun updateConversation(conversationId: String, title: String?, isPinned: Boolean?): Result<Unit>
    suspend fun deleteConversation(conversationId: String): Result<Unit>
    suspend fun getConversationHistory(conversationId: String, page: Int, pageSize: Int): Result<HistoryResult>
    suspend fun searchConversations(keyword: String, page: Int, pageSize: Int): Result<ConversationListResult>
    suspend fun getRunningConversations(): Result<List<String>>
    suspend fun getTitleSummary(conversationId: String, messages: List<HistoryMessage>): Result<String>
    suspend fun getFollowUpQuestions(conversationId: String): Result<List<String>>
    suspend fun getModeSupportModels(): Result<ModeSupportModelsResult>
    suspend fun getDailyHotTopics(): Result<TopicsResult>
    suspend fun getRecommendTopics(): Result<TopicsResult>
    suspend fun refreshRecommendTopics(): Result<TopicsResult>
    suspend fun toggleTts(enabled: Boolean): Result<Boolean>
    suspend fun imageOcr(imageUrl: String): Result<String>
    suspend fun checkPptUpgradeGate(): Result<PptUpgradeGateResult>
    suspend fun publishWebsite(conversationId: String, artifactId: String): Result<String>
    suspend fun getUserMaterials(): Result<MaterialsResult>
    suspend fun getUserVisuals(): Result<VisualsResult>
}

data class ConversationListResult(
    val conversations: List<ConversationSummary>,
    val pagination: Pagination?
)

data class ConversationSummary(
    val conversationId: String,
    val title: String,
    val lastMessage: String?,
    val updatedAt: String,
    val isPinned: Boolean,
    val isRunning: Boolean
)

data class ConversationDetail(
    val conversationId: String,
    val title: String,
    val createdAt: String,
    val updatedAt: String,
    val isPinned: Boolean,
    val messageCount: Int
)

data class HistoryMessage(
    val messageId: String,
    val role: String,
    val content: String,
    val blocks: List<StreamBlock>?,
    val createdAt: String
)

data class HistoryResult(
    val messages: List<HistoryMessage>,
    val pagination: Pagination?
)

data class Pagination(
    val page: Int = 1,
    val pageSize: Int = 20,
    val total: Long = 0,
    val hasMore: Boolean = false
)

data class ModeSupportModelsResult(
    val models: List<ModeSupportModel>
)

data class ModeSupportModel(
    val modelCode: String,
    val modelAlias: String,
    val modelType: String,
    val isOnline: Boolean,
    val subscriptionLevel: Int
)

data class TopicsResult(
    val topics: List<TopicItem>
)

data class TopicItem(
    val id: String,
    val title: String,
    val description: String?,
    val imageUrl: String?
)

data class PptUpgradeGateResult(
    val canAccess: Boolean,
    val requiredLevel: Int,
    val currentLevel: Int
)

data class MaterialsResult(
    val materials: List<MaterialItem>
)

data class MaterialItem(
    val id: String,
    val name: String,
    val url: String,
    val type: String
)

data class VisualsResult(
    val visuals: List<VisualItem>
)

data class VisualItem(
    val id: String,
    val imageUrl: String,
    val prompt: String?,
    val createdAt: String
)