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
| Chat, streaming, tools, vision input (flagship) | `agnes-2.5-flash` | `POST /v1/chat/completions` (512K ctx / 65.5K max output) **plus `POST /v1/responses`** (Responses API, documented for 2.5-flash) |
| Chat, coding, agents, vision | `agnes-2.0-flash` | `POST /v1/chat/completions` (256K ctx / 64K — after 2026-06 rollback from temporary 1M window) |
| Fast low-latency chat, titles, follow-ups | `agnes-1.5-flash` | `POST /v1/chat/completions` (~256K ctx / 64K) |
| Text→image / edit | `agnes-image-2.0-flash` | `POST /v1/images/generations` |
| High-detail image gen (recommended) | `agnes-image-2.1-flash` | `POST /v1/images/generations` |
| Text→video, image→video, keyframes (async) | `agnes-video-v2.0` | `POST /v1/videos`; poll via `GET https://apihub.agnes-ai.com/agnesapi?video_id=<id>` (never the legacy `/v1/videos/{task_id}` flow) |

> Doc pitfall: the vendor FAQ once claimed `agnes-2.0-flash`=512K ctx; catalog+changelog say 256K/64K. Trust catalog/console. Migration 2.0→2.5 = swap the model string only.

### 2.1 Chat request contract (`/v1/chat/completions`)
Params: `model` ✅, `messages` ✅ (content text **or** blocks `text`/`image_url`), `temperature`,
`top_p`, `max_tokens`, `stream`, `tools`+`tool_choice` (OpenAI function-calling),
`chat_template_kwargs` (extension field incl. thinking mode for OpenAI-style requests),
`thinking` (Anthropic-compatible requests). Vision input = **publicly reachable image URL only**
(content block `image_url`). Auth header `Authorization: Bearer $AGNES_API_KEY`, body
`application/json`.

### 2.2 Image request contract (`/v1/images/generations`)
| Param | Req | Notes |
|---|---|---|
| `model` | ✅ | `agnes-image-2.1-flash` (quality) / `2.0` (speed) |
| `prompt` | ✅ | description/editing instruction |
| `size` | ✅ | tier values **`1K`/`2K`/`3K`/`4K`** recommended; legacy exact sizes accepted, normalized |
| `ratio` | – | `1:1` default; `3:4` `4:3` `16:9` `9:16` `2:3` `3:2` `21:9` |
| `image[]` | for img2img | public URL(s) or data-URI base64; multiple = multi-image composition |
| `return_base64` | – | top-level base64 output for text→image |
| `extra_body.response_format` | – | `"url"` or `"b64_json"` — **MUST live in `extra_body`, not top-level (else 400)** |

Pitfalls: no separate img2img tag param; non-native sizes map to tiers (e.g. 1920×1080 →
`2K`+`16:9` ≈ 2624×1472). Dimension reference: 1:1 = 1024²/2048²/4096² (1K/2K/4K) ·
16:9 = 1312×736 → 2624×1472 → 5248×2944 · 9:16 mirrored · 21:9 = 1568×672 → 6272×2688.
Image RPM is **resolution-dependent** (§6.2).

### 2.3 Video request contract (`/v1/videos`, async) + polling
Params: `model`=`agnes-video-v2.0` ✅, `prompt` ✅, `image` (single public URL for image→video),
`mode` (`ti2vid`, `keyframes`; keyframes via `extra_body.image` URL-array + `extra_body.mode`),
`height`/`width` (defaults 768×1152; normalized to **480p/720p/1080p** tiers — trust response
fields `size`, `seconds`, `metadata.size_mapping`, not request values), `num_frames` (**≤441 and
8n+1**, e.g. 121 ≈ 5 s @24 fps), `frame_rate` 1–60, `num_inference_steps`, `seed`,
`negative_prompt`.
Create-response returns BOTH `task_id` and **`video_id`** plus `status/progress/seconds/size`.
**Poll with `video_id` only**, interval ≈5 s (avoid tight loops → 429); terminal statuses
`succeeded/success/completed/done` vs `failed/error/cancelled`.

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

## 6. Limits, quotas & error handling (catalog reference values)

### 6.1 Access types & quota accounting
Three key types, each with its **own limit pool**: free/default, enterprise-verified, Token Plan.
Rules: limits bind to the **key TYPE** — several keys of one type share ONE pool (no increase);
you may hold several types in parallel (separate pools); enterprise raises base RPM but grants
**no** Token-Plan quota (subscription required for that).
Quota units: text = per request, image = per generated image, video = per second of video.
Current vendor price note: images $0/img, video $0/s (limited via quotas; verify in console).

### 6.2 RPM reference values (verify in platform console before production)
| Traffic | Free (public / actual) | Enterprise | Token Plan |
|---|---|---|---|
| Text chat | 30 / 20 RPM | 60 / 40 RPM | 1000 / 1000 RPM |
| Image 1K | 30 / 20 | 60 / 40 | 100 (actual) |
| Image 2K | 20 / 10 | 40 / 20 | 80 (actual) |
| Image 3K | 2 / 1 | 2 / 1 | 1 |
| Image 4K | 1 / 1 | 1 / 1 | 1 |
| Video (2026-06-28 update) | 2 / 1 | 2 / 2 | 6 / 5 |

### 6.3 Token Plan subscription quotas
| Plan | Price (doc ref) | Text (`agnes-2.0-flash` class) | Images | Video |
|---|---|---|---|---|
| Starter | $4 | 1,500 req / 5h · 15,000 req / week | 4,000 img / day | 500 s / day |
| Plus | $10 | 7,500 req / 5h · 75,000 req / week | 4,000 img / day | 500 s / day |
| Pro | $50 | 30,000 req / 5h · 300,000 req / week | 4,000 img / day | 500 s / day |

Rebuild implication: your backend enforces the app's own plan mapping onto these pools;
surface remaining quota via the app's `QuotaLog` / credits endpoints (README §5.8).

### 6.4 Error-code matrix → app UX mapping (`GlobalErrorConfig`-style)
| Status | Cause (typical) | Backend action | App UX |
|---|---|---|---|
| 400 | malformed JSON, wrong params, context too long | validate + trim context | inline input error |
| 401 | bad/expired key, wrong account key | ops alert; never retry blindly | “service unavailable” fallback |
| 402 | balance/quota exhausted | map to upgrade gate | `PptUpgradeGateCheckResponse`-style paywall |
| 403 | model access not enabled / restricted region | check key permissions | feature-locked notice |
| 404 | wrong path/model, double `/v1`, stale `video_id` | fix config; no retry | generic error |
| 405 | wrong HTTP method | fix client | – |
| 408 | timeout (large payload/long task) | retry w/ backoff | spinner → retry chip |
| 409 | duplicate task / concurrent conflict | dedupe submission | disable double-tap |
| 413 | payload too large (big base64) | use image **URLs**, split payloads | compress before upload |
| 415 | unsupported media type/content-type | restrict to PNG/JPG/JPEG/WEBP | picker filter |
| 422 | param out of range (size/seed/ratio/frame) | clamp parameters | inline option error |
| 429 | RPM/concurrency exceeded | exponential backoff, queue | “slow down” toast |
| 431 | oversized headers | strip custom headers | – |
| 499 | client closed early (long task) | async/poll pattern | keep task running UI |
| 500 | internal error (bad geometry etc.) | retry minimal payload | generic error |
| 502/503/504 | gateway/upstream transient, wrong-model routing | backoff retry; check model routing/fallback | service busy notice |
| 520/522/524 | upstream/gateway timeouts (Cloudflare class) | backoff retry | service busy notice |

*Retry with exponential backoff only for: 408, 429, 500, 502, 503, 504, 520, 522, 524 —
never for 4xx semantic errors; never route-hop regions to fix 4xx (§1.1).*

## 7. Debugging telemetry (privacy-safe)

Bug reports collect: model, endpoint path, client version, **sanitized** request body, status
code + response body, request ID/timestamp — never API keys or user data.
