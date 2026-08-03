package com.agnes.sidecar

import com.agnes.sidecar.gateway.AgnesGatewayClient
import com.agnes.sidecar.routing.ModelRegistry
import com.agnes.sidecar.store.MockStore
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.contentnegotiation.*
import io.ktor.server.cors.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.serialization.*
import io.ktor.server.calllogging.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun Application.module() {
    install(CallLogging) {
        level = io.ktor.server.calllogging.Level.INFO
    }

    install(ContentNegotiation) {
        json(json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = true
        })
    }

    install(CORS) {
        anyHost()
        allowMethods = setOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        allowHeaders = setOf("Content-Type", "Authorization", "X-Requested-With")
        maxAge = io.ktor.http.HttpDuration.ofMinutes(10)
    }

    // Health check endpoint
    routing {
        get("/healthz") {
            call.respond(mapOf("status" to "ok", "service" to "agnes-sidecar"))
        }

        // API routes - mounted under /api/v1
        route("/api/v1") {
            // Auth routes
            route("auth") {
                get("me") {
                    call.respond(mapOf("code" to 0, "message" to "success", "data" to mapOf(
                        "id" to 1,
                        "username" to "mock_user",
                        "email" to "mock@example.com"
                    )))
                }
            }

            // Agnes chat routes
            route("agnes") {
                post("chat/stream") {
                    // In MOCK mode, return a mock SSE stream
                    val mockSseEmitter = MockSseEmitter()
                    mockSseEmitter.emitMockStream(call)
                }

                post("chat/stream/cancel") {
                    call.respond(mapOf("code" to 0, "message" to "cancelled"))
                }

                post("chat/stream/resume") {
                    val mockSseEmitter = MockSseEmitter()
                    mockSseEmitter.emitMockStream(call)
                }

                post("chat/stream/regenerate") {
                    val mockSseEmitter = MockSseEmitter()
                    mockSseEmitter.emitMockStream(call)
                }

                post("chat/stream/hitl-resume") {
                    val mockSseEmitter = MockSseEmitter()
                    mockSseEmitter.emitMockStream(call)
                }

                get("conversations") {
                    call.respond(mapOf("code" to 0, "message" to "success", "data" to mapOf(
                        "list" to listOf(
                            mapOf("conversation_id" to "conv_1", "title" to "Test Conversation", "last_message" to "Hello", "updated_at" to "2024-01-15T10:00:00Z", "is_pinned" to false, "is_running" to false)
                        ),
                        "pagination" to mapOf("page" to 1, "page_size" to 20, "total" to 1, "has_more" to false)
                    )))
                }

                post("conversation") {
                    call.respond(mapOf("code" to 0, "message" to "success", "data" to mapOf("conversation_id" to "conv_${System.currentTimeMillis()}")))
                }

                get("conversation") {
                    call.respond(mapOf("code" to 0, "message" to "success", "data" to mapOf(
                        "conversation_id" to "conv_1",
                        "title" to "Test Conversation",
                        "created_at" to "2024-01-01T00:00:00Z",
                        "updated_at" to "2024-01-15T10:00:00Z",
                        "is_pinned" to false,
                        "message_count" to 10
                    )))
                }

                get("conversation/history") {
                    call.respond(mapOf("code" to 0, "message" to "success", "data" to mapOf(
                        "messages" to listOf(
                            mapOf("message_id" to "msg_1", "role" to "user", "content" to "Hello", "blocks" to listOf(), "created_at" to "2024-01-15T10:00:00Z"),
                            mapOf("message_id" to "msg_2", "role" to "assistant", "content" to "Hi there!", "blocks" to listOf(), "created_at" to "2024-01-15T10:00:05Z")
                        ),
                        "pagination" to mapOf("page" to 1, "page_size" to 20, "total" to 2, "has_more" to false)
                    )))
                }

                get("follow-up-questions") {
                    call.respond(mapOf("code" to 0, "message" to "success", "data" to mapOf("questions" to listOf("Follow up 1", "Follow up 2"))))
                }

                get("mode_support_models") {
                    call.respond(mapOf("code" to 0, "message" to "success", "data" to mapOf("models" to listOf(
                        mapOf("model_code" to "agnes-2.5-flash", "model_alias" to "Agnes 2.5 Flash", "model_type" to "chat", "is_online" to true, "subscription_level" to 0),
                        mapOf("model_code" to "agnes-2.0-flash", "model_alias" to "Agnes 2.0 Flash", "model_type" to "chat", "is_online" to true, "subscription_level" to 1),
                        mapOf("model_code" to "agnes-1.5-flash", "model_alias" to "Agnes 1.5 Flash", "model_type" to "chat", "is_online" to true, "subscription_level" to 0)
                    ))))
                }

                post("conversation/title-summary") {
                    call.respond(mapOf("code" to 0, "message" to "success", "data" to mapOf("title" to "Generated Title")))
                }

                post("image_ocr") {
                    call.respond(mapOf("code" to 0, "message" to "success", "data" to mapOf("text" to "OCR extracted text")))
                }

                post("ppt/upgrade-gate/check") {
                    call.respond(mapOf("code" to 0, "message" to "success", "data" to mapOf("can_access" to true, "required_level" to 1, "current_level" to 2)))
                }

                get("user/materials") {
                    call.respond(mapOf("code" to 0, "message" to "success", "data" to mapOf("materials" to listOf(
                        mapOf("id" to "1", "name" to "Material 1", "url" to "https://example.com/mat1.png", "type" to "image"),
                        mapOf("id" to "2", "name" to "Material 2", "url" to "https://example.com/mat2.png", "type" to "image")
                    ))))
                }

                get("user/visuals") {
                    call.respond(mapOf("code" to 0, "message" to "success", "data" to mapOf("visuals" to listOf(
                        mapOf("id" to "1", "image_url" to "https://example.com/vis1.png", "prompt" to "A beautiful landscape", "created_at" to "2024-01-15T10:00:00Z"),
                        mapOf("id" to "2", "image_url" to "https://example.com/vis2.png", "prompt" to "Abstract art", "created_at" to "2024-01-14T10:00:00Z")
                    ))))
                }
            }

            // User routes
            route("user") {
                get("profile") {
                    call.respond(mapOf("code" to 0, "message" to "success", "data" to mapOf(
                        "id" to 1,
                        "username" to "mock_user",
                        "email" to "mock@example.com",
                        "avatar" to null,
                        "bio" to "Mock user bio",
                        "timezone" to "UTC",
                        "credits_balance" to 1000,
                        "subscription_level" to 1
                    )))
                }
            }

            // File upload routes
            route("file") {
                post("presigned-url") {
                    call.respond(mapOf("code" to 0, "message" to "success", "data" to mapOf(
                        "upload_url" to "https://storage.example.com/upload/abc123",
                        "file_id" to "file_${System.currentTimeMillis()}",
                        "expires_at" to (System.currentTimeMillis() / 1000 + 3600)
                    )))
                }
            }
        }
    }
}

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}