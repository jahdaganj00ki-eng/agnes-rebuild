package com.agnes.bundle_agnes.feature.chat.repository

import com.sobrr.agnes.feature_chat.model.ChatMessage
import com.sobrr.agnes.feature_chat.model.MessageRole
import com.sobrr.agnes.feature_chat.model.StreamBlock
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MockChatRepository : ChatRepository {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    override val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isStreaming = MutableStateFlow(false)
    override val isStreaming: StateFlow<Boolean> = _isStreaming

    override suspend fun sendMessage(request: ChatStreamRequestBody): Result<ReceiveChannel<StreamBlock>> {
        _isStreaming.value = true
        val channel = Channel<StreamBlock>(capacity = 10)

        // Simulate the SSE stream with mock blocks
        kotlinx.coroutines.launch {
            try {
                // Thinking block
                delay(300)
                channel.send(StreamBlock.ThinkingBlock("Analyzing the request...", false))

                delay(200)
                channel.send(StreamBlock.ThinkingBlock("Planning the response...", true))

                // Skill load block
                delay(300)
                channel.send(StreamBlock.SkillLoadBlock("image-generation", "Loading image generation skill"))

                // Tool call block
                delay(400)
                channel.send(StreamBlock.ToolCallBlock(
                    toolCallId = "call_${System.currentTimeMillis()}",
                    toolType = StreamBlock.ToolCallType.GenerateImage,
                    toolName = "generate_image",
                    arguments = mapOf("prompt" to request.message),
                    status = StreamBlock.ToolCallStatus.RUNNING
                ))

                delay(500)
                channel.send(StreamBlock.ToolCallBlock(
                    toolCallId = "call_${System.currentTimeMillis()}",
                    toolType = StreamBlock.ToolCallType.GenerateImage,
                    toolName = "generate_image",
                    arguments = mapOf("prompt" to request.message),
                    status = StreamBlock.ToolCallStatus.COMPLETED,
                    result = mapOf("image_url" to "https://example.com/generated_image.png")
                ))

                // Image block
                delay(200)
                channel.send(StreamBlock.ImageBlock(
                    imageUrl = "https://example.com/generated_image.png",
                    prompt = request.message,
                    width = 1024,
                    height = 1024,
                    format = "png"
                ))

                // Text blocks (streaming)
                val textResponse = "Here is the generated image based on your prompt: \"${request.message}\". The image has been created using the image generation skill."
                val words = textResponse.split(" ")
                var accumulated = ""
                for (word in words) {
                    delay(50)
                    accumulated += word + " "
                    channel.send(StreamBlock.TextBlock(accumulated.trim(), true))
                }

                // Followups
                delay(300)
                channel.send(StreamBlock.FollowupsBlock(
                    listOf("Can you modify the style?", "Generate another variation", "Download the image")
                ))

                // Done
                channel.send(StreamBlock.DoneBlock)

                // Add to messages
                val message = ChatMessage(
                    id = "msg_${System.currentTimeMillis()}",
                    role = MessageRole.ASSISTANT,
                    blocks = emptyList(), // Would be accumulated in real implementation
                    timestamp = System.currentTimeMillis()
                )
                _messages.update { it + message }
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
        delay(100)
        _isStreaming.value = false
        return Result.success(Unit)
    }

    override suspend fun resumeStream(conversationId: String, lastEventId: String): Result<ReceiveChannel<StreamBlock>> {
        // Simulate resuming from last event
        return sendMessage(ChatStreamRequestBody(conversationId, "Resuming...", modelCode = "mock"))
    }

    override suspend fun regenerateMessage(conversationId: String, messageId: String): Result<ReceiveChannel<StreamBlock>> {
        // Simulate regeneration
        return sendMessage(ChatStreamRequestBody(conversationId, "Regenerating...", modelCode = "mock"))
    }

    override suspend fun hitlResume(conversationId: String, hitlData: String, toolCallId: String): Result<ReceiveChannel<StreamBlock>> {
        // Simulate HITL resume
        return sendMessage(ChatStreamRequestBody(conversationId, "HITL resume: $hitlData", modelCode = "mock"))
    }

    // Conversation management
    override suspend fun getConversations(page: Int, pageSize: Int, keyword: String?): Result<ConversationListResult> {
        delay(200)
        val mockConversations = List(10) { i ->
            ConversationSummary(
                conversationId = "conv_$i",
                title = "Conversation ${i + 1}",
                lastMessage = "Last message in conversation ${i + 1}",
                updatedAt = "2024-01-${(i % 28) + 1}T10:00:00Z",
                isPinned = i == 0,
                isRunning = i == 1
            )
        }
        return Result.success(ConversationListResult(
            conversations = mockConversations,
            pagination = Pagination(page, pageSize, 100, page * pageSize < 100)
        ))
    }

    override suspend fun createConversation(title: String?, agentType: String?): Result<String> {
        delay(300)
        return Result.success("conv_${System.currentTimeMillis()}")
    }

    override suspend fun getConversationDetail(conversationId: String): Result<ConversationDetail> {
        delay(200)
        return Result.success(ConversationDetail(
            conversationId = conversationId,
            title = "Conversation Detail",
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-15T10:00:00Z",
            isPinned = false,
            messageCount = 50
        ))
    }

    override suspend fun updateConversation(conversationId: String, title: String?, isPinned: Boolean?): Result<Unit> {
        delay(200)
        return Result.success(Unit)
    }

    override suspend fun deleteConversation(conversationId: String): Result<Unit> {
        delay(200)
        return Result.success(Unit)
    }

    override suspend fun getConversationHistory(conversationId: String, page: Int, pageSize: Int): Result<HistoryResult> {
        delay(200)
        val messages = List(20) { i ->
            HistoryMessage(
                messageId = "msg_$i",
                role = if (i % 2 == 0) "user" else "assistant",
                content = "Message ${i + 1} in conversation",
                blocks = emptyList(),
                createdAt = "2024-01-${(i % 28) + 1}T10:${i}:00Z"
            )
        }
        return Result.success(HistoryResult(
            messages = messages,
            pagination = Pagination(page, pageSize, 100, page * pageSize < 100)
        ))
    }

    override suspend fun searchConversations(keyword: String, page: Int, pageSize: Int): Result<ConversationListResult> {
        delay(300)
        val filtered = List(5) { i ->
            ConversationSummary(
                conversationId = "search_$i",
                title = "Search result $i for '$keyword'",
                lastMessage = "Matching message...",
                updatedAt = "2024-01-15T10:00:00Z",
                isPinned = false,
                isRunning = false
            )
        }
        return Result.success(ConversationListResult(
            conversations = filtered,
            pagination = Pagination(page, pageSize, 5, false)
        ))
    }

    override suspend fun getRunningConversations(): Result<List<String>> {
        delay(100)
        return Result.success(listOf("conv_1", "conv_5"))
    }

    override suspend fun getTitleSummary(conversationId: String, messages: List<HistoryMessage>): Result<String> {
        delay(300)
        return Result.success("Generated title for conversation")
    }

    override suspend fun getFollowUpQuestions(conversationId: String): Result<List<String>> {
        delay(200)
        return Result.success(listOf("Follow up 1", "Follow up 2", "Follow up 3"))
    }

    override suspend fun getModeSupportModels(): Result<ModeSupportModelsResult> {
        delay(200)
        return Result.success(ModeSupportModelsResult(
            models = listOf(
                ModeSupportModel("agnes-2.5-flash", "Agnes 2.5 Flash", "chat", true, 0),
                ModeSupportModel("agnes-2.0-flash", "Agnes 2.0 Flash", "chat", true, 1),
                ModeSupportModel("agnes-1.5-flash", "Agnes 1.5 Flash", "chat", true, 0)
            )
        ))
    }

    override suspend fun getDailyHotTopics(): Result<TopicsResult> {
        delay(200)
        return Result.success(TopicsResult(
            topics = listOf(
                TopicItem("1", "AI News", "Latest AI developments", null),
                TopicItem("2", "Tech Trends", "Emerging technologies", null)
            )
        ))
    }

    override suspend fun getRecommendTopics(): Result<TopicsResult> {
        delay(200)
        return Result.success(TopicsResult(
            topics = listOf(
                TopicItem("1", "Creative Writing", "Write a story", null),
                TopicItem("2", "Code Review", "Review code snippets", null)
            )
        ))
    }

    override suspend fun refreshRecommendTopics(): Result<TopicsResult> {
        return getRecommendTopics()
    }

    override suspend fun toggleTts(enabled: Boolean): Result<Boolean> {
        delay(100)
        return Result.success(enabled)
    }

    override suspend fun imageOcr(imageUrl: String): Result<String> {
        delay(500)
        return Result.success("OCR text extracted from image")
    }

    override suspend fun checkPptUpgradeGate(): Result<PptUpgradeGateResult> {
        delay(200)
        return Result.success(PptUpgradeGateResult(true, 1, 2))
    }

    override suspend fun publishWebsite(conversationId: String, artifactId: String): Result<String> {
        delay(500)
        return Result.success("https://published.example.com/site_${System.currentTimeMillis()}")
    }

    override suspend fun getUserMaterials(): Result<MaterialsResult> {
        delay(200)
        return Result.success(MaterialsResult(
            materials = listOf(
                MaterialItem("1", "Material 1", "https://example.com/mat1.png", "image"),
                MaterialItem("2", "Material 2", "https://example.com/mat2.png", "image")
            )
        ))
    }

    override suspend fun getUserVisuals(): Result<VisualsResult> {
        delay(200)
        return Result.success(VisualsResult(
            visuals = listOf(
                VisualItem("1", "https://example.com/vis1.png", "A beautiful landscape", "2024-01-15T10:00:00Z"),
                VisualItem("2", "https://example.com/vis2.png", "Abstract art", "2024-01-14T10:00:00Z")
            )
        ))
    }
}