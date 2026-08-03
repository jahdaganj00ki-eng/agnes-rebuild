# AI Backend: Agnes AI Gateway Integration

The rebuild uses the **official Agnes AI OpenAI-compatible gateway** as the server-side AI
provider for chat, image and video generation. This document is updated against the attached
`AgnesAI-Komplettanalyse.md` reference (2026-08-03).

> Security rule: the Agnes gateway key is a backend secret. **Never ship it in the Android APK,
> WebView, JavaScript bundle, screenshots, logs or Git history.**

---

## 1. Required configuration

| Setting | Value / convention | Where it lives |
|---|---|---|
| API key | `AGNES_API_KEY` | backend secret/env only |
| Primary v1 base URL | `https://apihub.agnes-ai.com/v1` | backend HTTP client for chat/images/video-create |
| Video polling root | `https://apihub.agnes-ai.com` | backend HTTP client for `/agnesapi` polling |
| Auth header | `Authorization: Bearer <AGNES_API_KEY>` | backend interceptor |
| Content-Type | `application/json` | all JSON requests |

Official docs/account portal:

- Platform/API keys: `https://platform.agnes-ai.com/`
- International docs: `https://agnes-ai.com/`
- China site: `https://agnes-ai.cn/`

### 1.1 Regional endpoint routing

| Service route | Base URL | When to use |
|---|---|---|
| International primary | `https://apihub.agnes-ai.com/v1` | Default |
| International alternate | `https://apihub.agnes-ai.cn/v1` | Only after network/DNS/TLS/timeout failure on primary |
| China service | `https://api.agnes-ai.cn/v1` | China service accounts |

Routing rules:

- Ensure the configured base URL contains `/v1` **exactly once**.
- Do **not** route-hop for `400/401/403/422/429`; those indicate request, key, permission or
  quota problems on the selected service.
- For video polling, use the root host plus `/agnesapi?video_id=...`, not the `/v1` base.

---

## 2. Correct backend HTTP clients — no double `/v1`

If the v1 client is configured with:

```text
AGNES_BASE_URL=https://apihub.agnes-ai.com/v1
```

then relative paths must **not** include another `/v1`:

```kotlin
// Backend service only. Never place this client in the Android app.
// Retrofit baseUrl = "https://apihub.agnes-ai.com/v1/"
interface AgnesGatewayV1 {
    @Streaming
    @POST("chat/completions")
    suspend fun chat(@Body r: ChatRequest): ResponseBody

    @POST("responses")
    suspend fun responses(@Body r: ResponsesRequest): ResponsesResponse

    @POST("images/generations")
    suspend fun images(@Body r: ImageRequest): ImageResponse

    @POST("videos")
    suspend fun createVideo(@Body r: VideoRequest): VideoTask
}

// Separate root client for the current video polling endpoint.
// Retrofit baseUrl = "https://apihub.agnes-ai.com/"
interface AgnesGatewayRoot {
    @GET("agnesapi")
    suspend fun pollVideo(@Query("video_id") id: String): VideoResult
}
```

Alternative valid style: configure the base URL as `https://apihub.agnes-ai.com/` and use paths
`v1/chat/completions`, `v1/images/generations`, `v1/videos`. Choose one style and enforce it in
tests.

---

## 3. Architecture: mandatory backend proxy

```text
Android client ──(own REST, BaseResponse<T>, SSE blocks)──► YOUR backend
YOUR backend ──(OpenAI-compatible JSON/SSE, Bearer secret)──► Agnes gateway
```

The original app also keeps inference server-side: the client talks to an app API and receives
curated model metadata (`model_code`, `model_alias`, `model_type`, `is_online`,
`subscription_level`). The provider model names and API key belong in your backend.

Backend responsibilities:

1. Store and rotate `AGNES_API_KEY`.
2. Map app-facing `model_code` values to provider models.
3. Enforce plan/quota gates before generation.
4. Transform Agnes/OpenAI SSE deltas into the app stream block protocol (`thinking`,
   `skill_load`, `tool_call`, `text`, `image`, `artifact`, `followups`, `error`, `done`).
5. Sanitize telemetry and never log secrets or raw sensitive user content.

---

## 4. Model catalog and app-facing routing

### 4.1 Provider models

| Capability | Provider model | Endpoint | Notes |
|---|---|---|---|
| Chat, agents, tools, coding, vision | `agnes-2.5-flash` | `POST /v1/chat/completions` | Current default; 512K context, 65.5K max output |
| Chat fallback / legacy | `agnes-2.0-flash` | `POST /v1/chat/completions` | 256K context, 64K max output after rollback |
| Fast short completions | `agnes-1.5-flash` | `POST /v1/chat/completions` | Good for titles/follow-ups/simple chat |
| Responses API | `agnes-2.5-flash` | `POST /v1/responses` | Documented only for `agnes-2.5-flash` |
| Image generation/editing | `agnes-image-2.1-flash` | `POST /v1/images/generations` | Recommended default; high-detail, edit, multi-image |
| Fast image generation | `agnes-image-2.0-flash` | `POST /v1/images/generations` | Fast text-to-image fallback |
| Video generation | `agnes-video-v2.0` | `POST /v1/videos`; poll `/agnesapi?video_id=...` | Async; use `video_id` for polling |

### 4.2 App `model_code` → provider model mapping

The Android app should receive stable app-level model codes. The backend maps those codes to
provider models and can change providers without an APK update.

| App-facing `model_code` | Provider model | Use case |
|---|---|---|
| `chat_default` / `agent_default` | `agnes-2.5-flash` | Main agent/chat/tool/vision model |
| `chat_fast` | `agnes-1.5-flash` | Low-latency simple chat |
| `title_summary` | `agnes-1.5-flash` | Conversation titles |
| `followups` | `agnes-1.5-flash` | Suggested follow-up questions |
| `chat_legacy` | `agnes-2.0-flash` | Compatibility/fallback |
| `vision_default` | `agnes-2.5-flash` | Image URL input, OCR-adjacent tasks |
| `agnes-image` | `agnes-image-2.1-flash` | Existing app fallback alias |
| `image_default` | `agnes-image-2.1-flash` | Image Studio default |
| `image_edit` | `agnes-image-2.1-flash` | Reference-image editing/composition |
| `image_fast` | `agnes-image-2.0-flash` | Fast/cheap text-to-image |
| `video_default` | `agnes-video-v2.0` | Optional video feature |

Expose these via the app endpoint `GET /api/v1/agnes/mode_support_models` with fields such as
`model_code`, `model_alias`, `model_type`, `is_online`, `subscription_level`.

---

## 5. Request contracts to implement in the backend

### 5.1 Chat / text / agent / vision

Endpoint:

```text
POST {AGNES_BASE_URL}/chat/completions
```

Core fields:

| Parameter | Required | Notes |
|---|---:|---|
| `model` | yes | e.g. `agnes-2.5-flash` |
| `messages` | yes | OpenAI-compatible `system` / `user` / `assistant` messages |
| `temperature` | no | Sampling randomness |
| `top_p` | no | Nucleus sampling |
| `max_tokens` | no | Output cap |
| `stream` | no | `true` for app chat stream |
| `tools` / `tool_choice` | no | OpenAI-format function calling |
| `chat_template_kwargs` | no | Extension field, e.g. thinking mode for OpenAI-compatible requests |
| `thinking` | no | Thinking mode for Anthropic-compatible requests if supported by route |

Vision input uses publicly reachable image URLs:

```json
{
  "model": "agnes-2.5-flash",
  "messages": [
    {
      "role": "user",
      "content": [
        {"type": "text", "text": "Was ist auf diesem Bild?"},
        {"type": "image_url", "image_url": {"url": "https://example.com/image.jpg"}}
      ]
    }
  ],
  "stream": true
}
```

### 5.2 Image generation / editing

Endpoint:

```text
POST {AGNES_BASE_URL}/images/generations
```

Recommended request:

```json
{
  "model": "agnes-image-2.1-flash",
  "prompt": "Schwebende Stadt über einer Schlucht bei Sonnenaufgang, cineastisch",
  "size": "2K",
  "ratio": "16:9",
  "extra_body": {"response_format": "url"}
}
```

Image-to-image / multi-image edit:

```json
{
  "model": "agnes-image-2.1-flash",
  "prompt": "Behalte Pose und Gesicht, ändere den Stil zu Aquarell",
  "size": "2K",
  "ratio": "1:1",
  "image": ["https://cdn.example.com/uploads/reference.jpg"],
  "extra_body": {"response_format": "url"}
}
```

Rules to enforce:

- Prefer tier sizes: `1K`, `2K`, `3K`, `4K`.
- Supported ratios include `1:1`, `3:4`, `4:3`, `16:9`, `9:16`, `2:3`, `3:2`, `21:9`.
- Put `response_format` under `extra_body.response_format`; do **not** send it top-level.
- `image` is an array of public URLs or Data-URI Base64 strings.
- Do not use obsolete `tags: ["img2img"]`.
- Non-native exact sizes may be normalized by the service; use response dimensions/metadata.

Reference dimensions:

| Ratio | 1K | 2K | 4K |
|---|---|---|---|
| `1:1` | 1024×1024 | 2048×2048 | 4096×4096 |
| `16:9` | 1312×736 | 2624×1472 | 5248×2944 |
| `9:16` | 736×1312 | 1472×2624 | 2944×5248 |
| `21:9` | 1568×672 | 3136×1344 | 6272×2688 |

### 5.3 Video generation and polling

Create task:

```text
POST {AGNES_BASE_URL}/videos
```

```json
{
  "model": "agnes-video-v2.0",
  "prompt": "Katze am Strand bei Sonnenuntergang",
  "height": 768,
  "width": 1152,
  "num_frames": 121,
  "frame_rate": 24
}
```

Polling:

```text
GET https://apihub.agnes-ai.com/agnesapi?video_id=<VIDEO_ID>
```

Rules:

- Poll with `video_id`, not `task_id`.
- Poll about every 5 seconds; avoid tight loops to prevent `429`.
- `num_frames <= 441` and follows the `8n + 1` rule, e.g. `121`.
- Resolution may be normalized to 480p/720p/1080p tiers; trust response `size`, `seconds`,
  `metadata.size_mapping`.
- Terminal success statuses: `succeeded`, `success`, `completed`, `done`.
- Terminal failure statuses: `failed`, `error`, `cancelled`.

---

## 6. Feature → gateway mapping

| App/backend feature | Gateway call |
|---|---|
| `POST /api/v1/agnes/chat/stream` | `POST /v1/chat/completions`, `stream: true`, usually `agnes-2.5-flash` |
| `POST /api/v1/agnes/chat/stream/regenerate` | Re-issue last user turn via `/chat/completions` |
| `POST /api/v1/agnes/conversation/title-summary` | `agnes-1.5-flash` short completion |
| `GET /api/v1/agnes/follow-up-questions` | `agnes-1.5-flash` short completion |
| `POST /api/v1/agnes/image_ocr` | `agnes-2.5-flash` multimodal message with image URL |
| Character/personality/opening-line generation | `agnes-2.5-flash` or `agnes-2.0-flash`, structured output |
| Image Studio / edit-image tool | `agnes-image-2.1-flash`, `/images/generations` |
| Optional in-app video moments | `agnes-video-v2.0`, `/videos` + `/agnesapi?video_id=...` |

---

## 7. Limits, quotas and key pools

These are reference values from the public catalog/attached analysis; production values must be
verified in the platform console.

### 7.1 Text RPM

| Key/account type | Public RPM | Actual executable RPM |
|---|---:|---:|
| Free / default | 30 | 20 |
| Enterprise | 60 | 40 |
| Token Plan | 1,000 | 1,000 |

### 7.2 Image RPM by resolution tier

| Key/account type | 1K | 2K | 3K | 4K |
|---|---:|---:|---:|---:|
| Free / default, actual | 20 | 10 | 1 | 1 |
| Enterprise, actual | 40 | 20 | 1 | 1 |
| Token Plan, actual | 100 | 80 | 1 | 1 |

### 7.3 Video RPM

| Key/account type | Public RPM | Actual executable RPM |
|---|---:|---:|
| Free / default | 2 | 1 |
| Enterprise | 2 | 2 |
| Token Plan | 6 | 5 |

### 7.4 Token Plan subscription quotas

| Plan | Text | Images | Video |
|---|---|---:|---:|
| Starter | 1,500 requests / 5h; 15,000 / week | 4,000 / day | 500 s / day |
| Plus | 7,500 requests / 5h; 75,000 / week | 4,000 / day | 500 s / day |
| Pro | 30,000 requests / 5h; 300,000 / week | 4,000 / day | 500 s / day |

Accounting: text is per request, images per generated image, video per generated second. RPM and
quota apply simultaneously.

### 7.5 API-key pool rule

Limits are bound to the **key type/pool**, not to an individual key string. Multiple keys of the
same type do not increase throughput. Different account/key types can have separate pools.

---

## 8. Error handling and retries

Retry with exponential backoff only for: `408`, `429`, `500`, `502`, `503`, `504`, `520`, `522`,
`524`. Do not retry semantic 4xx errors blindly and do not route-hop for 4xx.

| Status | Typical cause | Backend action | App UX |
|---|---|---|---|
| 400 | malformed JSON, wrong params, `response_format` top-level | validate and fix request | inline input error |
| 401 | bad/missing key, wrong Bearer format | ops alert, rotate/check secret | service unavailable |
| 402 | balance/quota exhausted | map to upgrade/paywall | upgrade gate |
| 403 | model/region not enabled | check key permission | feature locked |
| 404 | wrong path/model, double `/v1`, stale `video_id` | fix config, no retry | generic error |
| 408 | timeout | retry/backoff | retry chip |
| 409 | duplicate/concurrent task | dedupe | disable double tap |
| 413 | payload too large | use URLs, compress/split | picker compression |
| 415 | wrong content type | restrict media | picker filter |
| 422 | parameter out of range | clamp options | inline option error |
| 429 | rate/concurrency limit | queue/backoff | slow-down toast |
| 500 | server/upstream failure | retry minimal payload | generic error |
| 502/503/504 | gateway/upstream transient | retry/backoff | service busy |
| 520/522/524 | gateway timeout class | retry/backoff | service busy |

---

## 9. Privacy-safe debugging telemetry

Collect only sanitized operational data:

- model/app `model_code`
- endpoint path
- client/backend version
- request ID and timestamp
- status code and sanitized response body
- sanitized request shape/metadata

Never collect/log:

- `AGNES_API_KEY`
- Authorization headers
- raw private files/images unless explicit diagnostic consent exists
- full user prompts in crash analytics by default
