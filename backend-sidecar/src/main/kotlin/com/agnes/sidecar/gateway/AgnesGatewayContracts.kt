package com.agnes.sidecar.gateway

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// OpenAI-compatible Chat Completion Request/Response
@Serializable
data class ChatMessage(
    val role: String,
    val content: String? = null,
    val images: List<String>? = null
)

@Serializable
data class ChatRequest(
    val model: String = "agnes-2.5-flash",
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
    val temperature: Float = 0.7f,
    val max_tokens: Int? = null
)

@Serializable
data class ChatChoice(
    val index: Int,
    val delta: ChatDelta? = null,
    val message: ChatMessage? = null,
    val finish_reason: String? = null
)

@Serializable
data class ChatDelta(
    val role: String? = null,
    val content: String? = null
)

@Serializable
data class ChatCompletionResponse(
    val id: String,
    val object: String = "chat.completion.chunk",
    val created: Long = System.currentTimeMillis() / 1000,
    val model: String,
    val choices: List<ChatChoice>
)

// Image Generation
@Serializable
data class ImageRequest(
    val model: String = "agnes-image-2.1-flash",
    val prompt: String,
    val n: Int = 1,
    val size: String = "1024x1024",
    val quality: String = "standard",
    val style: String = "vivid",
    val response_format: String = "url",
    val extra_body: Map<String, Any>? = null
)

@Serializable
data class ImageData(
    val url: String? = null,
    val b64_json: String? = null,
    val revised_prompt: String? = null
)

@Serializable
data class ImageResponse(
    val created: Long = System.currentTimeMillis() / 1000,
    val data: List<ImageData>
)

// Video Generation
@Serializable
data class VideoRequest(
    val model: String = "agnes-video-v2.0",
    val prompt: String,
    val num_frames: Int = 81,
    val width: Int = 512,
    val height: Int = 512,
    val fps: Int = 8
)

@Serializable
data class VideoTask(
    val video_id: String,
    val status: String = "pending",
    val created_at: Long = System.currentTimeMillis() / 1000
)

@Serializable
data class VideoResult(
    val video_id: String,
    val status: String,
    val video_url: String? = null,
    val error: String? = null
)

// Gateway Client Interface
interface AgnesGatewayClient {
    suspend fun chatCompletion(request: ChatRequest): ChatCompletionResponse
    suspend fun chatCompletionStream(request: ChatRequest): kotlinx.coroutines.channels.ReceiveChannel<ChatCompletionResponse>
    suspend fun imageGeneration(request: ImageRequest): ImageResponse
    suspend fun videoGeneration(request: VideoRequest): VideoTask
    suspend fun videoPoll(videoId: String): VideoResult
}

// Gateway client implementation using Ktor client
class DefaultAgnesGatewayClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val pollBaseUrl: String = "https://apihub.agnes-ai.com"
) : AgnesGatewayClient {

    private val client = io.ktor.client.HttpClient(io.ktor.client.engine.cio.CIO) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true; prettyPrint = true })
        }
        install(io.ktor.client.plugins.auth.Authentication) {
            bearer { token = apiKey }
        }
        install(io.ktor.client.plugins.logging.Logging) {
            level = io.ktor.client.plugins.logging.Level.HEADERS
        }
    }

    override suspend fun chatCompletion(request: ChatRequest): ChatCompletionResponse {
        return client.post("$baseUrl/chat/completions") {
            contentType(io.ktor.http.ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun chatCompletionStream(request: ChatRequest): kotlinx.coroutines.channels.ReceiveChannel<ChatCompletionResponse> {
        val channel = kotlinx.coroutines.channels.Channel<ChatCompletionResponse>()

        kotlinx.coroutines.launch {
            try {
                client.executeRequest {
                    method = io.ktor.http.HttpMethod.Post
                    url("$baseUrl/chat/completions")
                    contentType(io.ktor.http.ContentType.Application.Json)
                    setBody(request)
                    headers.append("Accept", "text/event-stream")
                } response: io.ktor.client.statement.HttpResponse {
                    response.bodyAsChannel().map { byteReadChannel ->
                        val text = byteReadChannel.readText()
                        text.split("\n").forEach { line ->
                            if (line.startsWith("data: ")) {
                                val json = line.substring(6)
                                if (json != "[DONE]") {
                                    try {
                                        val chunk = Json { ignoreUnknownKeys = true }.decodeFromString<ChatCompletionResponse>(json)
                                        channel.send(chunk)
                                    } catch (e: Exception) {
                                        // Ignore parsing errors
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                channel.close(e)
            } finally {
                channel.close()
            }
        }

        return channel
    }

    override suspend fun imageGeneration(request: ImageRequest): ImageResponse {
        return client.post("$baseUrl/images/generations") {
            contentType(io.ktor.http.ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun videoGeneration(request: VideoRequest): VideoTask {
        return client.post("$baseUrl/videos") {
            contentType(io.ktor.http.ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun videoPoll(videoId: String): VideoResult {
        return client.get("$pollBaseUrl/agnesapi") {
            parameter("video_id", videoId)
        }
    }
}