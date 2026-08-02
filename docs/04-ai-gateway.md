# AI Backend: Agnes AI Gateway Integration

The rebuild uses the **official Agnes AI OpenAI-compatible gateway** (documented in
`AgnesAI-Labs/AgnesAI-Models`) as the server-side AI provider for chat, image and video
generation. This replaces implementing your own LLM backend.

## 1. What you need

| Requirement | Details |
|---|---|
| API key | Register at `https://platform.agnes-ai.com/`, apply for a key, then send it as `Authorization: Bearer <key>` |
| Official docs | `https://agnes-ai.com/doc/overview` |
| Env conventions | `AGNES_API_KEY` (secret), `AGNES_BASE_URL` (selected route, see §1.1) |
| Note | The docs repo contains **docs + examples only** — no model weights or code to vendor; limits and availability may change, confirm production values in the platform console |

### 1.1 Regional endpoint routing

| Service route | Base URL | When to use |
|---|---|---|
| International (primary) | `https://apihub.agnes-ai.com/v1` | Default |
| International (alternate) | `https://apihub.agnes-ai.cn/v1` | Only when the primary route has a network/DNS/TLS/timeout failure |
| China service | `https://api.agnes-ai.cn/v1` | China service accounts |

Routing rules:
* Ensure the URL ends in `/v1` and clients do **not** append `/v1` a second time.
* Test the alternate international route with one minimal request; keep the reachable route in `AGNES_BASE_URL`.
* **Never** switch routes in response to `400/401/403/422/429` — those are request/key/permission/quota issues of the *selected* service, not routing problems.
* Image API root stays the route's host without `/v1` suffix for non-OpenAI endpoints.

## 2. Model catalog (public reference, check for updates)

| Capability | Model | Endpoint |
|---|---|---|
| Chat, streaming, tools, vision input (flagship) | `agnes-2.5-flash` | `POST /v1/chat/completions` (≈512K ctx) |
| Chat, coding, agents, vision | `agnes-2.0-flash` | `POST /v1/chat/completions` (≈256K ctx; the temporary 1M window was rolled back 2026-06) |
| Fast low-latency chat | `agnes-1.5-flash` | `POST /v1/chat/completions` (≈256K ctx, 64K max output) |
| Text→image / edit | `agnes-image-2.0-flash` | `POST /v1/images/generations` |
| High-detail image gen | `agnes-image-2.1-flash` | `POST /v1/images/generations` |
| Text→video, image→video, keyframes (async) | `agnes-video-v2.0` | `POST /v1/videos`; poll result via `GET https://apihub.agnes-ai.com/agnesapi?video_id=<id>` (use `video_id`, not the legacy `task_id` flow) |

## 3. Architecture (mandatory: proxy through your own backend)

```
Android client ──(BaseResponse<T> envelope, own REST)──► YOUR backend ──(Bearer key)──► apihub.agnes-ai.com
```

* **Never ship the gateway API key inside the APK.** This matches the original app's design:
  its client only talks to its own backend (`api.agnes-ai.com`) and receives curated model
  metadata (`model_code`, `model_alias`, `model_type`, `is_online`, `subscription_level`).
* Your backend additionally enforces subscription gating (`ModelsAccess*`/`ModelsCost*`
  endpoints of this spec) and converts between the app's streaming block protocol and the
  gateway's SSE format.

## 4. Feature → gateway mapping

| Spec feature (docs/01-api-surface.md) | Gateway call |
|---|---|
| `POST /chat/stream` (streaming assistant turns) | `POST /v1/chat/completions` with `stream: true`, model `agnes-2.5-flash` |
| `POST /chat/regenerate` | same, re-issue last user turn |
| `POST /conversation/title` | `agnes-1.5-flash` short-completion |
| Follow-up questions / preset replies | `agnes-1.5-flash` |
| `POST /role/avatar`, `POST /generate/candidate-images` | `POST /v1/images/generations` (`agnes-image-2.1-flash`) |
| `POST /generate/personality-brief`, `POST /generate/opening-line` | `agnes-2.0-flash` (structured prompt) |
| Game help answers, UGC content generation | `agnes-2.0-flash` / `agnes-2.5-flash` |
| Vision: `ImageElementRecognition*`, OCR-adjacent tasks | `POST /v1/chat/completions` with image URL input (multimodal) |
| In-app video moments (if added) | `POST /v1/videos` + `video_id` polling |

## 5. Minimal server-side contract (implement in YOUR backend)

```kotlin
// OpenAI-compatible chat request/response shapes (standard wire format)
data class ChatMessage(val role: String, val content: List<Part>?)
data class Part(val type: String, val text: String? = null, val image_url: ImageUrl? = null)
data class ImageUrl(val url: String)
data class ChatRequest(val model: String = "agnes-2.5-flash",
                       val messages: List<ChatMessage>,
                       val stream: Boolean = true)
```

```kotlin
// Retrofit interface in YOUR backend service only (never in the Android app).
// Backend config: apiKey from env AGNES_API_KEY, baseUrl from env AGNES_BASE_URL
// (default "https://apihub.agnes-ai.com/v1", see §1.1 routing).
interface AgnesGateway {
    @Streaming @POST("/v1/chat/completions") suspend fun chat(@Body r: ChatRequest): ResponseBody
    @POST("/v1/images/generations") suspend fun images(@Body r: ImageRequest): ImageResponse
    @POST("/v1/videos") suspend fun createVideo(@Body r: VideoRequest): VideoTask
    @GET("/agnesapi") suspend fun pollVideo(@Query("video_id") id: String): VideoResult
}
```

The backend converts the SSE delta stream into the app's design-system block protocol (see
README §5.2) and injects `subscription_level` checks before every generation call.

## 6. Error, retry & quota handling

* **Retry with exponential backoff only for:** `408, 429, 500, 502, 503, 504, 520, 522, 524`.
  Everything else (`400/401/403/422`) means fix the request, key or account — do not retry and
  do not switch regional routes (§1.1).
* Map gateway error codes onto the app's `GlobalErrorConfig` pattern; surface quota exhaustion
  as the existing `PptUpgradeGateCheckResponse` upgrade-gate flow.
* Honor per-tier RPM limits from the live catalog; treat them as changeable reference values.
* Video: poll with `video_id`; treat >few-minutes queue time as failure → user retry.
* Debugging/bug reports collect: model, endpoint, client version, sanitized request body, status
  code + response body, request ID/timestamp — **never** API keys or user data.
