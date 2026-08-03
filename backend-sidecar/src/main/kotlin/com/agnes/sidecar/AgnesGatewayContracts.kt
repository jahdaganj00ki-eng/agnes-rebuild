package com.agnes.sidecar

/**
 * Contract sketch for the backend-sidecar implementation.
 *
 * This file intentionally contains no API key and no Android client code. A real service can adapt
 * these DTOs to Retrofit, Ktor client, Spring WebClient, http4k, etc.
 */

object AgnesGatewayDefaults {
    const val BASE_URL = "https://apihub.agnes-ai.com/v1"
    const val POLL_BASE_URL = "https://apihub.agnes-ai.com"

    const val CHAT_MODEL = "agnes-2.5-flash"
    const val FAST_MODEL = "agnes-1.5-flash"
    const val LEGACY_MODEL = "agnes-2.0-flash"
    const val IMAGE_MODEL = "agnes-image-2.1-flash"
    const val IMAGE_FAST_MODEL = "agnes-image-2.0-flash"
    const val VIDEO_MODEL = "agnes-video-v2.0"
}

data class ChatRequest(
    val model: String = AgnesGatewayDefaults.CHAT_MODEL,
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
    val temperature: Double? = null,
    val top_p: Double? = null,
    val max_tokens: Int? = null,
    val tools: List<Map<String, Any?>>? = null,
    val tool_choice: Any? = null,
    val chat_template_kwargs: Map<String, Any?>? = null,
    val thinking: Map<String, Any?>? = null,
)

data class ChatMessage(
    val role: String,
    val content: Any,
)

data class TextPart(
    val type: String = "text",
    val text: String,
)

data class ImageUrlPart(
    val type: String = "image_url",
    val image_url: ImageUrl,
)

data class ImageUrl(
    val url: String,
)

data class ImageRequest(
    val model: String = AgnesGatewayDefaults.IMAGE_MODEL,
    val prompt: String,
    val size: String = "2K",
    val ratio: String = "1:1",
    val image: List<String>? = null,
    val return_base64: Boolean? = null,
    val extra_body: ImageExtraBody = ImageExtraBody(response_format = "url"),
)

data class ImageExtraBody(
    val response_format: String = "url", // "url" or "b64_json"; keep nested under extra_body
)

data class VideoRequest(
    val model: String = AgnesGatewayDefaults.VIDEO_MODEL,
    val prompt: String,
    val image: String? = null,
    val mode: String? = null,
    val height: Int = 768,
    val width: Int = 1152,
    val num_frames: Int = 121, // must be <= 441 and follow 8n+1
    val frame_rate: Double = 24.0,
    val num_inference_steps: Int? = null,
    val seed: Int? = null,
    val negative_prompt: String? = null,
    val extra_body: Map<String, Any?>? = null,
)

data class VideoTask(
    val id: String? = null,
    val task_id: String? = null,
    val video_id: String,
    val status: String,
    val progress: Double? = null,
    val seconds: String? = null,
    val size: String? = null,
)

data class VideoResult(
    val video_id: String? = null,
    val status: String,
    val url: String? = null,
    val error: String? = null,
    val progress: Double? = null,
    val metadata: Map<String, Any?>? = null,
)

object ModelRouter {
    private val providerByAppCode = mapOf(
        "chat_default" to AgnesGatewayDefaults.CHAT_MODEL,
        "agent_default" to AgnesGatewayDefaults.CHAT_MODEL,
        "chat_fast" to AgnesGatewayDefaults.FAST_MODEL,
        "title_summary" to AgnesGatewayDefaults.FAST_MODEL,
        "followups" to AgnesGatewayDefaults.FAST_MODEL,
        "chat_legacy" to AgnesGatewayDefaults.LEGACY_MODEL,
        "vision_default" to AgnesGatewayDefaults.CHAT_MODEL,
        "agnes-image" to AgnesGatewayDefaults.IMAGE_MODEL,
        "image_default" to AgnesGatewayDefaults.IMAGE_MODEL,
        "image_edit" to AgnesGatewayDefaults.IMAGE_MODEL,
        "image_fast" to AgnesGatewayDefaults.IMAGE_FAST_MODEL,
        "video_default" to AgnesGatewayDefaults.VIDEO_MODEL,
    )

    fun providerModel(appModelCode: String): String =
        providerByAppCode[appModelCode]
            ?: error("Unsupported app model_code: $appModelCode")
}

fun validateVideoFrames(numFrames: Int) {
    require(numFrames <= 441) { "num_frames must be <= 441" }
    require((numFrames - 1) % 8 == 0) { "num_frames must follow the 8n+1 rule" }
}

fun isRetryableGatewayStatus(status: Int): Boolean =
    status in setOf(408, 429, 500, 502, 503, 504, 520, 522, 524)
