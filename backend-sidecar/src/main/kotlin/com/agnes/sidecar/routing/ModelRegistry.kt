package com.agnes.sidecar.routing

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class ModelRoutingConfig(
    val chat_stream: String = "agnes-2.5-flash",
    val chat_regenerate: String = "agnes-2.5-flash",
    val chat_hitl: String = "agnes-2.5-flash",
    val tools_vision: String = "agnes-2.5-flash",
    val title_summary: String = "agnes-1.5-flash",
    val follow_ups: String = "agnes-1.5-flash",
    val persona: String = "agnes-2.0-flash",
    val opening_line: String = "agnes-2.0-flash",
    val game_help: String = "agnes-2.0-flash",
    val image_ocr: String = "agnes-2.5-flash",
    val image_generation: String = "agnes-image-2.1-flash",
    val image_edit: String = "agnes-image-2.1-flash",
    val image_fallback: String = "agnes-image-2.0-flash",
    val video_generation: String = "agnes-video-v2.0",
    val gateway_base_url: String = "https://apihub.agnes-ai.com/v1",
    val gateway_poll_base_url: String = "https://apihub.agnes-ai.com",
    val gateway_alt_base_url: String = "https://apihub.agnes-ai.cn/v1",
    val gateway_cn_base_url: String = "https://api.agnes-ai.cn/v1",
    val retry: RetryConfig = RetryConfig(),
    val quota: QuotaConfig = QuotaConfig()
)

@Serializable
data class RetryConfig(
    val codes: List<Int> = listOf(408, 429, 500, 502, 503, 504, 520, 522, 524),
    val max_attempts: Int = 3,
    val base_delay_ms: Long = 1000,
    val max_delay_ms: Long = 30000,
    val jitter: Boolean = true
)

@Serializable
data class QuotaConfig(
    val free: AccessTypeQuota = AccessTypeQuota(),
    val enterprise: AccessTypeQuota = AccessTypeQuota(
        text_rpm = 40,
        image_rpm = mapOf("1K" to 40, "2K" to 20, "3K" to 1, "4K" to 1),
        video_rpm = 2,
        video_daily_seconds = 500
    ),
    val token_plan_starter: PlanQuota = PlanQuota(),
    val token_plan_plus: PlanQuota = PlanQuota(
        text_5h = 7500,
        text_weekly = 75000,
        image_daily = 4000,
        video_daily_seconds = 500
    ),
    val token_plan_pro: PlanQuota = PlanQuota(
        text_5h = 30000,
        text_weekly = 300000,
        image_daily = 4000,
        video_daily_seconds = 500
    )
)

@Serializable
data class AccessTypeQuota(
    val text_rpm: Int = 20,
    val image_rpm: Map<String, Int> = mapOf("1K" to 20, "2K" to 10, "3K" to 1, "4K" to 1),
    val video_rpm: Int = 1,
    val video_daily_seconds: Int = 500
)

@Serializable
data class PlanQuota(
    val text_5h: Int = 1500,
    val text_weekly: Int = 15000,
    val image_daily: Int = 4000,
    val video_daily_seconds: Int = 500
)

object ModelRegistry {
    private var config: ModelRoutingConfig = ModelRoutingConfig()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    init {
        loadFromFile()
    }

    private fun loadFromFile() {
        val yamlFile = File("model-routing.yaml")
        if (yamlFile.exists()) {
            try {
                val yamlContent = yamlFile.readText()
                // Simple YAML parsing for our config
                // In production, use a proper YAML parser like SnakeYAML
                parseSimpleYaml(yamlContent)
            } catch (e: Exception) {
                println("Warning: Failed to load model-routing.yaml, using defaults: ${e.message}")
            }
        }
    }

    private fun parseSimpleYaml(content: String) {
        // Simple key-value parsing for our flat config
        content.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains(":")) {
                val parts = trimmed.split(":", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim()
                        .trim('"')
                        .trim("'")
                    setConfigValue(key, value)
                }
            }
        }
    }

    private fun setConfigValue(key: String, value: String) {
        when (key) {
            "chat_stream" -> config = config.copy(chat_stream = value)
            "chat_regenerate" -> config = config.copy(chat_regenerate = value)
            "chat_hitl" -> config = config.copy(chat_hitl = value)
            "tools_vision" -> config = config.copy(tools_vision = value)
            "title_summary" -> config = config.copy(title_summary = value)
            "follow_ups" -> config = config.copy(follow_ups = value)
            "persona" -> config = config.copy(persona = value)
            "opening_line" -> config = config.copy(opening_line = value)
            "game_help" -> config = config.copy(game_help = value)
            "image_ocr" -> config = config.copy(image_ocr = value)
            "image_generation" -> config = config.copy(image_generation = value)
            "image_edit" -> config = config.copy(image_edit = value)
            "image_fallback" -> config = config.copy(image_fallback = value)
            "video_generation" -> config = config.copy(video_generation = value)
            "gateway_base_url" -> config = config.copy(gateway_base_url = value)
            "gateway_poll_base_url" -> config = config.copy(gateway_poll_base_url = value)
            "gateway_alt_base_url" -> config = config.copy(gateway_alt_base_url = value)
            "gateway_cn_base_url" -> config = config.copy(gateway_cn_base_url = value)
            "PORT" -> { /* handled by env */ }
            "PROFILE" -> { /* handled by env */ }
        }
    }

    fun getModelFor(task: String): String = when (task) {
        "chat_stream", "chat_regenerate", "chat_hitl", "tools_vision" -> config.chat_stream
        "title_summary", "follow_ups" -> config.title_summary
        "persona", "opening_line", "game_help" -> config.persona
        "image_ocr" -> config.image_ocr
        "image_generation" -> config.image_generation
        "image_edit" -> config.image_edit
        "video_generation" -> config.video_generation
        else -> config.chat_stream
    }

    fun getGatewayBaseUrl(): String = config.gateway_base_url
    fun getGatewayPollBaseUrl(): String = config.gateway_poll_base_url
    fun getRetryConfig(): RetryConfig = config.retry
    fun getQuotaConfig(): QuotaConfig = config.quota
}