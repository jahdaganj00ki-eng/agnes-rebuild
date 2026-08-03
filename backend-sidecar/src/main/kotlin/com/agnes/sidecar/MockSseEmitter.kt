package com.agnes.sidecar

import io.ktor.http.ContentType
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class MockSseEmitter {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun emitMockStream(call: io.ktor.server.request.ApplicationCall) {
        call.respondText(
            contentType = ContentType.Text.EventStream,
            status = io.ktor.http.HttpStatusCode.OK
        ) {
            // Send the mock SSE stream with the 9 block types
            val blocks = generateMockBlocks()

            for (block in blocks) {
                val jsonStr = json.encodeToString(block)
                send("data: $jsonStr\n\n")
                delay(200)
            }
            send("data: [DONE]\n\n")
        }
    }

    private fun generateMockBlocks(): List<Any> {
        return listOf(
            mapOf(
                "type" to "thinking",
                "content" to "Analyzing the user's request...",
                "is_expanded" to false
            ),
            mapOf(
                "type" to "thinking",
                "content" to "Planning the response structure...",
                "is_expanded" to true
            ),
            mapOf(
                "type" to "skill_load",
                "skill_name" to "image-generation",
                "skill_description" to "Loading image generation skill for text-to-image"
            ),
            mapOf(
                "type" to "tool_call",
                "tool_call_id" to "call_${System.currentTimeMillis()}",
                "tool_type" to "GenerateImage",
                "tool_name" to "generate_image",
                "arguments" to mapOf("prompt" to "A beautiful sunset over mountains"),
                "status" to "running"
            ),
            mapOf(
                "type" to "tool_call",
                "tool_call_id" to "call_${System.currentTimeMillis()}",
                "tool_type" to "GenerateImage",
                "tool_name" to "generate_image",
                "arguments" to mapOf("prompt" to "A beautiful sunset over mountains"),
                "status" to "completed",
                "result" to mapOf("image_url" to "https://example.com/generated.png")
            ),
            mapOf(
                "type" to "image",
                "image_url" to "https://example.com/generated.png",
                "prompt" to "A beautiful sunset over mountains",
                "width" to 1024,
                "height" to 1024,
                "format" to "png"
            ),
            mapOf(
                "type" to "text",
                "content" to "Here is the generated image based on your prompt. The image shows a beautiful sunset over mountains with vibrant colors.",
                "delta" to false
            ),
            mapOf(
                "type" to "followups",
                "questions" to listOf("Can you make it more vibrant?", "Generate a night version", "Change the perspective")
            ),
            mapOf(
                "type" to "done"
            )
        )
    }
}