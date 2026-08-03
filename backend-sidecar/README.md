# Backend Sidecar — Agnes Gateway Proxy

This module is the intended place for all direct calls to the Agnes AI gateway. It is a skeleton
contract for the coding agent/backend implementation; do not put gateway access in the Android app.

```text
Android app ──► backend-sidecar / your backend ──► Agnes AI gateway
```

## Required environment

Copy `.env.example` to your deployment secret store and provide real values there. Never commit
real secrets.

```bash
AGNES_API_KEY=...
AGNES_BASE_URL=https://apihub.agnes-ai.com/v1
AGNES_POLL_BASE_URL=https://apihub.agnes-ai.com
AGNES_CHAT_MODEL=agnes-2.5-flash
AGNES_FAST_MODEL=agnes-1.5-flash
AGNES_LEGACY_MODEL=agnes-2.0-flash
AGNES_IMAGE_MODEL=agnes-image-2.1-flash
AGNES_IMAGE_FAST_MODEL=agnes-image-2.0-flash
AGNES_VIDEO_MODEL=agnes-video-v2.0
```

## App-facing endpoints to implement first

| App endpoint | Sidecar action | Agnes gateway |
|---|---|---|
| `POST /api/v1/agnes/chat/stream` | Build conversation context, apply tools, stream block protocol | `POST /v1/chat/completions` |
| `POST /api/v1/agnes/chat/stream/regenerate` | Re-run last user turn | `POST /v1/chat/completions` |
| `POST /api/v1/agnes/conversation/title-summary` | Short title completion | `POST /v1/chat/completions` with `agnes-1.5-flash` |
| `GET /api/v1/agnes/follow-up-questions` | Short follow-up completion | `POST /v1/chat/completions` with `agnes-1.5-flash` |
| `POST /api/v1/agnes/image_ocr` | Vision message with image URL | `POST /v1/chat/completions` with `agnes-2.5-flash` |
| Image Studio / edit-image | Prompt + optional reference image array | `POST /v1/images/generations` |
| Optional video feature | Create async task and poll by `video_id` | `POST /v1/videos`, `GET /agnesapi?video_id=...` |

## Implementation notes

- Use two HTTP clients or two Retrofit base URLs:
  - `AGNES_BASE_URL` ending in `/v1` for `chat/completions`, `responses`, `images/generations`, `videos`.
  - `AGNES_POLL_BASE_URL` without `/v1` for `/agnesapi?video_id=...`.
- Never double-append `/v1`.
- Put image `response_format` under `extra_body.response_format`.
- Poll video with `video_id`, not `task_id`.
- Add exponential backoff only for retryable statuses documented in `../docs/04-ai-gateway.md`.
- Log sanitized metadata only; never log `Authorization` or `AGNES_API_KEY`.

## Files in this skeleton

- `.env.example` — safe environment template.
- `model-routing.yaml` — app `model_code` to provider-model mapping.
- `src/main/kotlin/com/agnes/sidecar/AgnesGatewayContracts.kt` — DTO/interface contract sketch.
