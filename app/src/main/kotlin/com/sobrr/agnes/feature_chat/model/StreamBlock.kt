package com.sobrr.agnes.feature_chat.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

// Stream block types from the SSE stream
@Serializable
sealed class StreamBlock {
    @Serializable
    data class ThinkingBlock(
        @SerialName("content") val content: String,
        @SerialName("is_expanded") val isExpanded: Boolean = false
    ) : StreamBlock()

    @Serializable
    data class SkillLoadBlock(
        @SerialName("skill_name") val skillName: String,
        @SerialName("skill_description") val skillDescription: String?
    ) : StreamBlock()

    @Serializable
    data class ToolCallBlock(
        @SerialName("tool_call_id") val toolCallId: String,
        @SerialName("tool_type") val toolType: ToolCallType,
        @SerialName("tool_name") val toolName: String,
        @SerialName("arguments") val arguments: Map<String, Any>,
        @SerialName("status") val status: ToolCallStatus = ToolCallStatus.PENDING,
        @SerialName("result") val result: Map<String, Any>? = null
    ) : StreamBlock()

    @Serializable
    data class TextBlock(
        @SerialName("content") val content: String,
        @SerialName("delta") val delta: Boolean = false
    ) : StreamBlock()

    @Serializable
    data class ImageBlock(
        @SerialName("image_url") val imageUrl: String,
        @SerialName("prompt") val prompt: String?,
        @SerialName("width") val width: Int?,
        @SerialName("height") val height: Int?,
        @SerialName("format") val format: String?
    ) : StreamBlock()

    @Serializable
    data class ArtifactBlock(
        @SerialName("artifact_id") val artifactId: String,
        @SerialName("artifact_type") val artifactType: String,
        @SerialName("title") val title: String,
        @SerialName("preview_url") val previewUrl: String?,
        @SerialName("download_url") val downloadUrl: String?,
        @SerialName("metadata") val metadata: Map<String, Any>?
    ) : StreamBlock()

    @Serializable
    data class FollowupsBlock(
        @SerialName("questions") val questions: List<String>
    ) : StreamBlock()

    @Serializable
    data class ErrorBlock(
        @SerialName("code") val code: Int,
        @SerialName("message") val message: String,
        @SerialName("recoverable") val recoverable: Boolean = true
    ) : StreamBlock()

    @Serializable
    object DoneBlock : StreamBlock()

    // Type discriminator for serialization
    fun getType(): String = when (this) {
        is ThinkingBlock -> "thinking"
        is SkillLoadBlock -> "skill_load"
        is ToolCallBlock -> "tool_call"
        is TextBlock -> "text"
        is ImageBlock -> "image"
        is ArtifactBlock -> "artifact"
        is FollowupsBlock -> "followups"
        is ErrorBlock -> "error"
        is DoneBlock -> "done"
    }
}

@Serializable
enum class ToolCallType {
    @SerialName("LoadSkill") LoadSkill,
    @SerialName("GenerateImage") GenerateImage,
    @SerialName("WebSearch") WebSearch,
    @SerialName("ImageSearch") ImageSearch,
    @SerialName("ReadFile") ReadFile,
    @SerialName("WriteFile") WriteFile,
    @SerialName("EditFile") EditFile,
    @SerialName("ListFiles") ListFiles,
    @SerialName("Execute") Execute,
    @SerialName("WriteReport") WriteReport,
    @SerialName("QueryWeather") QueryWeather,
    @SerialName("ProfileData") ProfileData,
    @SerialName("Other") Other
}

@Serializable
enum class ToolCallStatus {
    @SerialName("pending") PENDING,
    @SerialName("running") RUNNING,
    @SerialName("completed") COMPLETED,
    @SerialName("failed") FAILED
}

// Chat message for UI
@Serializable
data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val blocks: List<StreamBlock>,
    val timestamp: Long,
    val isStreaming: Boolean = false
) {
    fun getTextContent(): String {
        return blocks
            .filterIsInstance<StreamBlock.TextBlock>()
            .joinToString("") { it.content }
    }
}

enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}