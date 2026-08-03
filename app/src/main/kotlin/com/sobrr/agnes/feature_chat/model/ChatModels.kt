package com.sobrr.agnes.feature_chat.model

import com.google.gson.annotations.SerializedName
import com.sobrr.agnes.data.model.base.BaseResponse
import com.sobrr.agnes.data.model.base.Pagination
import com.sobrr.agnes.data.model.base.BaseNoResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// Chat Stream Request/Response
data class ChatStreamRequestBody(
    @SerializedName("conversation_id") val conversationId: String?,
    @SerializedName("message") val message: String,
    @SerializedName("tool_mode") val toolMode: String? = null,
    @SerializedName("scene") val scene: String? = null,
    @SerializedName("agent_type") val agentType: String? = null,
    @SerializedName("attachments") val attachments: List<Attachment>? = null,
    @SerializedName("model_code") val modelCode: String? = null
)

data class Attachment(
    @SerializedName("type") val type: String,
    @SerializedName("url") val url: String,
    @SerializedName("file_id") val fileId: String?
)

data class ChatRegenerateRequestBody(
    @SerializedName("conversation_id") val conversationId: String,
    @SerializedName("message_id") val messageId: String
)

data class ChatResumeStreamRequestBody(
    @SerializedName("conversation_id") val conversationId: String,
    @SerializedName("last_event_id") val lastEventId: String
)

data class ChatHitlResumeRequestBody(
    @SerializedName("conversation_id") val conversationId: String,
    @SerializedName("hitl_data") val hitlData: String,
    @SerializedName("tool_call_id") val toolCallId: String
)

// Conversation models
data class ConversationListRequest(
    @SerializedName("page") val page: Int = 1,
    @SerializedName("page_size") val pageSize: Int = 20,
    @SerializedName("keyword") val keyword: String? = null
)

data class ConversationSummary(
    @SerializedName("conversation_id") val conversationId: String,
    @SerializedName("title") val title: String,
    @SerializedName("last_message") val lastMessage: String?,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("is_pinned") val isPinned: Boolean,
    @SerializedName("is_running") val isRunning: Boolean
)

data class AgnesMultiConversationList(
    @SerializedName("list") val list: List<ConversationSummary>,
    @SerializedName("pagination") val pagination: Pagination?
)

data class ConversationDetail(
    @SerializedName("conversation_id") val conversationId: String,
    @SerializedName("title") val title: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("is_pinned") val isPinned: Boolean,
    @SerializedName("message_count") val messageCount: Int
)

data class CreateConversationRequest(
    @SerializedName("title") val title: String? = null,
    @SerializedName("agent_type") val agentType: String? = null
)

data class CreateConversationResponse(
    @SerializedName("conversation_id") val conversationId: String
)

data class UpdateConversationRequest(
    @SerializedName("title") val title: String?,
    @SerializedName("is_pinned") val isPinned: Boolean?
)

data class ConversationHistoryResponse(
    @SerializedName("messages") val messages: List<HistoryMessage>,
    @SerializedName("pagination") val pagination: Pagination?
)

data class HistoryMessage(
    @SerializedName("message_id") val messageId: String,
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String,
    @SerializedName("blocks") val blocks: List<StreamBlock>?,
    @SerializedName("created_at") val createdAt: String
)

data class ConversationSearchResponse(
    @SerializedName("results") val results: List<ConversationSummary>,
    @SerializedName("pagination") val pagination: Pagination?
)

data class ConversationRunningResponse(
    @SerializedName("running_conversations") val runningConversations: List<String>
)

data class ConversationTitleSummaryRequest(
    @SerializedName("conversation_id") val conversationId: String,
    @SerializedName("messages") val messages: List<HistoryMessage>
)

data class ConversationTitleSummaryResponse(
    @SerializedName("title") val title: String
)

data class FollowUpQuestionsResponse(
    @SerializedName("questions") val questions: List<String>
)

data class ModeSupportModelsResponse(
    @SerializedName("models") val models: List<ModeSupportModel>
)

data class ModeSupportModel(
    @SerializedName("model_code") val modelCode: String,
    @SerializedName("model_alias") val modelAlias: String,
    @SerializedName("model_type") val modelType: String,
    @SerializedName("is_online") val isOnline: Boolean,
    @SerializedName("subscription_level") val subscriptionLevel: Int
)

data class TopicsResponse(
    @SerializedName("topics") val topics: List<TopicItem>
)

data class TopicItem(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("image_url") val imageUrl: String?
)

data class AgnesImageOcrRequest(
    @SerializedName("image_url") val imageUrl: String
)

data class AgnesImageOcrResponse(
    @SerializedName("text") val text: String
)

data class PptUpgradeGateCheckResponse(
    @SerializedName("can_access") val canAccess: Boolean,
    @SerializedName("required_level") val requiredLevel: Int,
    @SerializedName("current_level") val currentLevel: Int
)

data class TtsToggleRequest(
    @SerializedName("enabled") val enabled: Boolean
)

data class TtsToggleResponse(
    @SerializedName("enabled") val enabled: Boolean
)

// ChatApi interface
interface ChatApi {

    @POST("api/v1/agnes/chat/stream")
    suspend fun streamChat(@Body request: ChatStreamRequestBody): okhttp3.ResponseBody

    @POST("api/v1/agnes/chat/stream/cancel")
    suspend fun cancelStream(@Body request: CancelStreamRequest): BaseNoResponse

    @POST("api/v1/agnes/chat/stream/resume")
    suspend fun resumeStream(@Body request: ChatResumeStreamRequestBody): okhttp3.ResponseBody

    @POST("api/v1/agnes/chat/stream/regenerate")
    suspend fun regenerateStream(@Body request: ChatRegenerateRequestBody): okhttp3.ResponseBody

    @POST("api/v1/agnes/chat/stream/hitl-resume")
    suspend fun hitlResumeStream(@Body request: ChatHitlResumeRequestBody): okhttp3.ResponseBody

    @GET("api/v1/agnes/conversations")
    suspend fun getConversations(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("keyword") keyword: String? = null
    ): BaseResponse<AgnesMultiConversationList>

    @POST("api/v1/agnes/conversation")
    suspend fun createConversation(@Body request: CreateConversationRequest): BaseResponse<CreateConversationResponse>

    @GET("api/v1/agnes/conversation")
    suspend fun getConversationDetail(@Query("conversation_id") conversationId: String): BaseResponse<ConversationDetail>

    @PATCH("api/v1/agnes/conversation")
    suspend fun updateConversation(
        @Query("conversation_id") conversationId: String,
        @Body request: UpdateConversationRequest
    ): BaseNoResponse

    @DELETE("api/v1/agnes/conversation")
    suspend fun deleteConversation(@Query("conversation_id") conversationId: String): BaseNoResponse

    @GET("api/v1/agnes/conversation/history")
    suspend fun getConversationHistory(
        @Query("conversation_id") conversationId: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): BaseResponse<ConversationHistoryResponse>

    @GET("api/v1/agnes/conversation/search")
    suspend fun searchConversations(
        @Query("keyword") keyword: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): BaseResponse<ConversationSearchResponse>

    @GET("api/v1/agnes/conversation/running")
    suspend fun getRunningConversations(): BaseResponse<ConversationRunningResponse>

    @POST("api/v1/agnes/conversation/title-summary")
    suspend fun getTitleSummary(@Body request: ConversationTitleSummaryRequest): BaseResponse<ConversationTitleSummaryResponse>

    @GET("api/v1/agnes/agnes-chats")
    suspend fun getAgnesChats(): BaseResponse<AgnesMultiConversationList>

    @GET("api/v1/agnes/follow-up-questions")
    suspend fun getFollowUpQuestions(
        @Query("conversation_id") conversationId: String
    ): BaseResponse<FollowUpQuestionsResponse>

    @GET("api/v1/agnes/mode_support_models")
    suspend fun getModeSupportModels(): BaseResponse<ModeSupportModelsResponse>

    @GET("api/v1/agnes/daily-hot-topics/latest")
    suspend fun getDailyHotTopics(): BaseResponse<TopicsResponse>

    @GET("api/v1/agnes/recommend-topics")
    suspend fun getRecommendTopics(): BaseResponse<TopicsResponse>

    @POST("api/v1/agnes/recommend-topics/refresh")
    suspend fun refreshRecommendTopics(): BaseResponse<TopicsResponse>

    @POST("api/v1/agnes/ai_voice/tts-toggle")
    suspend fun toggleTts(@Body request: TtsToggleRequest): BaseResponse<TtsToggleResponse>

    @POST("api/v1/agnes/image_ocr")
    suspend fun imageOcr(@Body request: AgnesImageOcrRequest): BaseResponse<AgnesImageOcrResponse>

    @POST("api/v1/agnes/ppt/upgrade-gate/check")
    suspend fun checkPptUpgradeGate(): BaseResponse<PptUpgradeGateCheckResponse>

    @POST("api/v1/agnes/website/publish")
    suspend fun publishWebsite(@Body request: PublishWebsiteRequest): BaseResponse<PublishWebsiteResponse>

    @GET("api/v1/agnes/user/materials")
    suspend fun getUserMaterials(): BaseResponse<UserMaterialsResponse>

    @GET("api/v1/agnes/user/visuals")
    suspend fun getUserVisuals(): BaseResponse<UserVisualsResponse>
}

data class CancelStreamRequest(
    @SerializedName("conversation_id") val conversationId: String
)

data class PublishWebsiteRequest(
    @SerializedName("conversation_id") val conversationId: String,
    @SerializedName("artifact_id") val artifactId: String
)

data class PublishWebsiteResponse(
    @SerializedName("url") val url: String
)

data class UserMaterialsResponse(
    @SerializedName("materials") val materials: List<MaterialItem>
)

data class MaterialItem(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("url") val url: String,
    @SerializedName("type") val type: String
)

data class UserVisualsResponse(
    @SerializedName("visuals") val visuals: List<VisualItem>
)

data class VisualItem(
    @SerializedName("id") val id: String,
    @SerializedName("image_url") val imageUrl: String,
    @SerializedName("prompt") val prompt: String?,
    @SerializedName("created_at") val createdAt: String
)