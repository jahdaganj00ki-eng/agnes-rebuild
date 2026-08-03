package com.agnes.bundle_agnes.feature.chat.repository

import com.agnes.bundle_agnes.core.di.AppContainer
import com.sobrr.agnes.data.model.base.BaseResponse
import com.sobrr.agnes.data.network.ApiResult
import com.sobrr.agnes.feature_chat.model.ChatApi
import com.sobrr.agnes.feature_chat.model.ChatStreamRequestBody
import com.sobrr.agnes.feature_chat.model.StreamBlock
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

class NetworkChatRepository(
    private val chatApi: ChatApi,
    private val appContainer: AppContainer
) : ChatRepository {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    override val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isStreaming = MutableStateFlow(false)
    override val isStreaming: StateFlow<Boolean> = _isStreaming

    override suspend fun sendMessage(request: ChatStreamRequestBody): Result<ReceiveChannel<StreamBlock>> {
        _isStreaming.value = true
        val channel = Channel<StreamBlock>(capacity = 10)

        kotlinx.coroutines.launch {
            try {
                val response = chatApi.streamChat(request)
                // Parse SSE stream
                val parser = SseStreamParser()
                response.source().readAll().forEach { buffer ->
                    val chunk = buffer.readUtf8()
                    parser.parseChunk(chunk).forEach { block ->
                        channel.send(block)
                    }
                }
                channel.send(StreamBlock.DoneBlock)
            } catch (e: Exception) {
                channel.send(StreamBlock.ErrorBlock(-1, e.message ?: "Stream error", true))
            } finally {
                channel.close()
                _isStreaming.value = false
            }
        }

        return Result.success(channel)
    }

    override suspend fun cancelStream(conversationId: String): Result<Unit> {
        val request = com.sobrr.agnes.feature_chat.model.CancelStreamRequest(conversationId)
        val result = chatApi.cancelStream(request)
        return handleApiResult(result)
    }

    override suspend fun resumeStream(conversationId: String, lastEventId: String): Result<ReceiveChannel<StreamBlock>> {
        val request = com.sobrr.agnes.feature_chat.model.ChatResumeStreamRequestBody(conversationId, lastEventId)
        val channel = Channel<StreamBlock>(capacity = 10)

        kotlinx.coroutines.launch {
            try {
                _isStreaming.value = true
                val response = chatApi.resumeStream(request)
                val parser = SseStreamParser()
                response.source().readAll().forEach { buffer ->
                    val chunk = buffer.readUtf8()
                    parser.parseChunk(chunk).forEach { block ->
                        channel.send(block)
                    }
                }
                channel.send(StreamBlock.DoneBlock)
            } catch (e: Exception) {
                channel.send(StreamBlock.ErrorBlock(-1, e.message ?: "Stream error", true))
            } finally {
                channel.close()
                _isStreaming.value = false
            }
        }

        return Result.success(channel)
    }

    override suspend fun regenerateMessage(conversationId: String, messageId: String): Result<ReceiveChannel<StreamBlock>> {
        val request = com.sobrr.agnes.feature_chat.model.ChatRegenerateRequestBody(conversationId, messageId)
        val channel = Channel<StreamBlock>(capacity = 10)

        kotlinx.coroutines.launch {
            try {
                _isStreaming.value = true
                val response = chatApi.regenerateStream(request)
                val parser = SseStreamParser()
                response.source().readAll().forEach { buffer ->
                    val chunk = buffer.readUtf8()
                    parser.parseChunk(chunk).forEach { block ->
                        channel.send(block)
                    }
                }
                channel.send(StreamBlock.DoneBlock)
            } catch (e: Exception) {
                channel.send(StreamBlock.ErrorBlock(-1, e.message ?: "Stream error", true))
            } finally {
                channel.close()
                _isStreaming.value = false
            }
        }

        return Result.success(channel)
    }

    override suspend fun hitlResume(conversationId: String, hitlData: String, toolCallId: String): Result<ReceiveChannel<StreamBlock>> {
        val request = com.sobrr.agnes.feature_chat.model.ChatHitlResumeRequestBody(conversationId, hitlData, toolCallId)
        val channel = Channel<StreamBlock>(capacity = 10)

        kotlinx.coroutines.launch {
            try {
                _isStreaming.value = true
                val response = chatApi.hitlResumeStream(request)
                val parser = SseStreamParser()
                response.source().readAll().forEach { buffer ->
                    val chunk = buffer.readUtf8()
                    parser.parseChunk(chunk).forEach { block ->
                        channel.send(block)
                    }
                }
                channel.send(StreamBlock.DoneBlock)
            } catch (e: Exception) {
                channel.send(StreamBlock.ErrorBlock(-1, e.message ?: "Stream error", true))
            } finally {
                channel.close()
                _isStreaming.value = false
            }
        }

        return Result.success(channel)
    }

    // Conversation management
    override suspend fun getConversations(page: Int, pageSize: Int, keyword: String?): Result<ConversationListResult> {
        val result = chatApi.getConversations(page, pageSize, keyword)
        return handleApiResult(result).map { resp ->
            ConversationListResult(
                conversations = resp.data?.list?.map { dto ->
                    ConversationSummary(
                        conversationId = dto.conversationId,
                        title = dto.title,
                        lastMessage = dto.lastMessage,
                        updatedAt = dto.updatedAt,
                        isPinned = dto.isPinned,
                        isRunning = dto.isRunning
                    )
                } ?: emptyList(),
                pagination = resp.data?.pagination
            )
        }
    }

    override suspend fun createConversation(title: String?, agentType: String?): Result<String> {
        val request = com.sobrr.agnes.feature_chat.model.CreateConversationRequest(title, agentType)
        val result = chatApi.createConversation(request)
        return handleApiResult(result).map { it.data?.conversationId ?: "" }
    }

    override suspend fun getConversationDetail(conversationId: String): Result<ConversationDetail> {
        val result = chatApi.getConversationDetail(conversationId)
        return handleApiResult(result).map { dto ->
            ConversationDetail(
                conversationId = dto.conversationId,
                title = dto.title,
                createdAt = dto.createdAt,
                updatedAt = dto.updatedAt,
                isPinned = dto.isPinned,
                messageCount = dto.messageCount
            )
        }
    }

    override suspend fun updateConversation(conversationId: String, title: String?, isPinned: Boolean?): Result<Unit> {
        val request = com.sobrr.agnes.feature_chat.model.UpdateConversationRequest(title, isPinned)
        val result = chatApi.updateConversation(conversationId, request)
        return handleApiResult(result)
    }

    override suspend fun deleteConversation(conversationId: String): Result<Unit> {
        val result = chatApi.deleteConversation(conversationId)
        return handleApiResult(result)
    }

    override suspend fun getConversationHistory(conversationId: String, page: Int, pageSize: Int): Result<HistoryResult> {
        val result = chatApi.getConversationHistory(conversationId, page, pageSize)
        return handleApiResult(result).map { resp ->
            HistoryResult(
                messages = resp.data?.messages?.map { dto ->
                    HistoryMessage(
                        messageId = dto.messageId,
                        role = dto.role,
                        content = dto.content,
                        blocks = dto.blocks,
                        createdAt = dto.createdAt
                    )
                } ?: emptyList(),
                pagination = resp.data?.pagination
            )
        }
    }

    override suspend fun searchConversations(keyword: String, page: Int, pageSize: Int): Result<ConversationListResult> {
        val result = chatApi.searchConversations(keyword, page, pageSize)
        return handleApiResult(result).map { resp ->
            ConversationListResult(
                conversations = resp.data?.results?.map { dto ->
                    ConversationSummary(
                        conversationId = dto.conversationId,
                        title = dto.title,
                        lastMessage = dto.lastMessage,
                        updatedAt = dto.updatedAt,
                        isPinned = dto.isPinned,
                        isRunning = dto.isRunning
                    )
                } ?: emptyList(),
                pagination = resp.data?.pagination
            )
        }
    }

    override suspend fun getRunningConversations(): Result<List<String>> {
        val result = chatApi.getRunningConversations()
        return handleApiResult(result).map { resp -> resp.data?.runningConversations ?: emptyList() }
    }

    override suspend fun getTitleSummary(conversationId: String, messages: List<HistoryMessage>): Result<String> {
        val request = com.sobrr.agnes.feature_chat.model.ConversationTitleSummaryRequest(conversationId, messages)
        val result = chatApi.getTitleSummary(request)
        return handleApiResult(result).map { it.data?.title ?: "" }
    }

    override suspend fun getFollowUpQuestions(conversationId: String): Result<List<String>> {
        val result = chatApi.getFollowUpQuestions(conversationId)
        return handleApiResult(result).map { it.data?.questions ?: emptyList() }
    }

    override suspend fun getModeSupportModels(): Result<ModeSupportModelsResult> {
        val result = chatApi.getModeSupportModels()
        return handleApiResult(result).map { resp ->
            ModeSupportModelsResult(
                models = resp.data?.models?.map { dto ->
                    ModeSupportModel(
                        modelCode = dto.modelCode,
                        modelAlias = dto.modelAlias,
                        modelType = dto.modelType,
                        isOnline = dto.isOnline,
                        subscriptionLevel = dto.subscriptionLevel
                    )
                } ?: emptyList()
            )
        }
    }

    override suspend fun getDailyHotTopics(): Result<TopicsResult> {
        val result = chatApi.getDailyHotTopics()
        return handleApiResult(result).map { resp ->
            TopicsResult(
                topics = resp.data?.topics?.map { dto ->
                    TopicItem(dto.id, dto.title, dto.description, dto.imageUrl)
                } ?: emptyList()
            )
        }
    }

    override suspend fun getRecommendTopics(): Result<TopicsResult> {
        val result = chatApi.getRecommendTopics()
        return handleApiResult(result).map { resp ->
            TopicsResult(
                topics = resp.data?.topics?.map { dto ->
                    TopicItem(dto.id, dto.title, dto.description, dto.imageUrl)
                } ?: emptyList()
            )
        }
    }

    override suspend fun refreshRecommendTopics(): Result<TopicsResult> {
        val result = chatApi.refreshRecommendTopics()
        return handleApiResult(result).map { resp ->
            TopicsResult(
                topics = resp.data?.topics?.map { dto ->
                    TopicItem(dto.id, dto.title, dto.description, dto.imageUrl)
                } ?: emptyList()
            )
        }
    }

    override suspend fun toggleTts(enabled: Boolean): Result<Boolean> {
        val request = com.sobrr.agnes.feature_chat.model.TtsToggleRequest(enabled)
        val result = chatApi.toggleTts(request)
        return handleApiResult(result).map { it.data?.enabled ?: enabled }
    }

    override suspend fun imageOcr(imageUrl: String): Result<String> {
        val request = com.sobrr.agnes.feature_chat.model.AgnesImageOcrRequest(imageUrl)
        val result = chatApi.imageOcr(request)
        return handleApiResult(result).map { it.data?.text ?: "" }
    }

    override suspend fun checkPptUpgradeGate(): Result<PptUpgradeGateResult> {
        val result = chatApi.checkPptUpgradeGate()
        return handleApiResult(result).map { dto ->
            PptUpgradeGateResult(
                canAccess = dto.canAccess,
                requiredLevel = dto.requiredLevel,
                currentLevel = dto.currentLevel
            )
        }
    }

    override suspend fun publishWebsite(conversationId: String, artifactId: String): Result<String> {
        val request = com.sobrr.agnes.feature_chat.model.PublishWebsiteRequest(conversationId, artifactId)
        val result = chatApi.publishWebsite(request)
        return handleApiResult(result).map { it.data?.url ?: "" }
    }

    override suspend fun getUserMaterials(): Result<MaterialsResult> {
        val result = chatApi.getUserMaterials()
        return handleApiResult(result).map { resp ->
            MaterialsResult(
                materials = resp.data?.materials?.map { dto ->
                    MaterialItem(dto.id, dto.name, dto.url, dto.type)
                } ?: emptyList()
            )
        }
    }

    override suspend fun getUserVisuals(): Result<VisualsResult> {
        val result = chatApi.getUserVisuals()
        return handleApiResult(result).map { resp ->
            VisualsResult(
                visuals = resp.data?.visuals?.map { dto ->
                    VisualItem(dto.id, dto.imageUrl, dto.prompt, dto.createdAt)
                } ?: emptyList()
            )
        }
    }

    private fun <T> handleApiResult(result: ApiResult<BaseResponse<T>>): Result<T> {
        return when (result) {
            is ApiResult.Success -> {
                if (result.data.isSuccess()) {
                    result.data.data?.let { Result.success(it) } ?: Result.failure(IllegalStateException("Empty data"))
                } else {
                    Result.failure(ApiException(result.data.code, result.data.message))
                }
            }
            is ApiResult.Error -> Result.failure(ApiException(result.code, result.message))
        }
    }
}

class ApiException(val code: Int, val message: String) : Exception(message)

// SSE Stream Parser
class SseStreamParser {
    private var buffer = StringBuilder()

    fun parseChunk(chunk: String): List<StreamBlock> {
        buffer.append(chunk)
        val blocks = mutableListOf<StreamBlock>()

        val lines = buffer.toString().split("\n")
        // Keep the last line in buffer if it doesn't end with newline
        buffer = StringBuilder(lines.last())
        // Process all complete lines except the last (incomplete) one
        for (line in lines.dropLast(1)) {
            parseLine(line).let { block ->
                if (block != null) blocks.add(block)
            }
        }

        return blocks
    }

    private fun parseLine(line: String): StreamBlock? {
        if (line.startsWith("event: ")) {
            // Event type line - we'll use the next data line
            return null
        }
        if (line.startsWith("data: ")) {
            val json = line.substring(6)
            if (json == "[DONE]") return StreamBlock.DoneBlock
            return parseJson(json)
        }
        return null
    }

    private fun parseJson(json: String): StreamBlock? {
        // In a real implementation, use kotlinx.serialization or Gson
        // For now, return a placeholder
        return try {
            // Parse JSON to StreamBlock
            // This would use kotlinx.serialization.json.Json.decodeFromString<StreamBlock>(json)
            StreamBlock.TextBlock(json, false)
        } catch (e: Exception) {
            null
        }
    }
}