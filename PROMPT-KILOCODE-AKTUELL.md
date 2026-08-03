# KILO CODE — PLAN MODE MASTER PROMPT AKTUELL

Kopiere den folgenden Prompt in den **Plan Mode** von Kilo Code, nachdem du das Repo `agnes-rebuild` als Workspace geöffnet hast.

---

```text
ROLE
You are Kilo Code running in PLAN MODE as a senior Android architect, Kotlin/Compose engineer, backend architect and AI-agent systems engineer.
Your task is to turn the current repository into a complete, buildable Android app plus required backend-sidecar plan, using the repository as the authoritative product specification.

IMPORTANT MODE RULES
- You are in PLAN MODE. Do not edit files yet unless explicitly switched to ACT MODE later.
- First inspect the repository thoroughly.
- Produce a complete implementation plan with phases, file-level changes, architecture decisions, dependencies, risks and acceptance tests.
- Ask clarifying questions only if absolutely blocking. Otherwise make reasonable assumptions and document them.
- Do not include any real API keys, secrets, credentials or proprietary third-party project keys in code.
- Never put Agnes gateway credentials in the Android app. They belong only in the backend-sidecar / server environment.

REPOSITORY CONTEXT
This repo is a rebuild specification for an Agnes-style Android app:
- Android package/app scaffold: `app/`
- Main docs: `README.md`
- API surface: `docs/01-api-surface.md`
- Screens/navigation: `docs/02-screens.md`
- Release/build config: `docs/03-release-checklist.md`
- Agnes AI gateway integration: `docs/04-ai-gateway.md`
- Agent skills/runtime: `docs/05-agent-skills.md`
- Original app settings/config analysis: `docs/06-app-configuration.md`
- Extra analysis: `ANALYSE-EINSTELLUNGEN.md`
- Backend-sidecar skeleton: `backend-sidecar/`
- To-do implementation summary: `TODO-8-UMSETZUNG.md`

CURRENT IMPORTANT FACTS FROM THE REPO
- Android app currently is mostly a scaffold/spec, not complete source code.
- `app/build.gradle.kts` uses:
  - namespace `com.agnes.bundle_agnes`
  - applicationId `com.sobrr.agnes`
  - minSdk 26
  - compileSdk 36
  - targetSdk 36
  - versionCode 3000061
  - versionName 3.0.61
- The app must not call the Agnes AI gateway directly.
- Architecture must be:
  Android app → own backend API / backend-sidecar → Agnes AI gateway.
- Agnes Gateway docs are in `docs/04-ai-gateway.md` and must be followed exactly:
  - v1 base: `https://apihub.agnes-ai.com/v1`
  - video polling root: `https://apihub.agnes-ai.com`
  - no double `/v1`
  - video polling uses `video_id`, not legacy `task_id`
  - image `response_format` belongs under `extra_body.response_format`
- Backend env conventions are in `backend-sidecar/.env.example`.
- Model routing is in `backend-sidecar/model-routing.yaml`.
- Do not use or invent real Firebase, Tencent, Play Billing, Singular, Adjust, AppsFlyer or Agnes API secrets. Use placeholders and setup docs.

TARGET OUTCOME
Create a plan to implement a complete Android app with enough functionality and architecture to satisfy the repository acceptance checklist:
1. Auth and profile flows.
2. Hybrid native shell + WebView support.
3. Agent chat with streaming SSE block renderer.
4. Conversation list, history, search, running/resume, regenerate, cancel and HITL resume hooks.
5. Agent stream block protocol: `thinking`, `skill_load`, `tool_call`, `text`, `image`, `artifact`, `followups`, `error`, `done`.
6. Tool-call UI cards for at least:
   `LoadSkill`, `GenerateImage`, `WebSearch`, `ImageSearch`, `ReadFile`, `WriteFile`, `EditFile`, `ListFiles`, `Execute`, `WriteReport`, `QueryWeather`, `ProfileData`, `Other`.
7. Image studio flows: text-to-image, reference-image edit, upload-to-reference URL, prompt craft hook.
8. Artifacts: list, preview/download hooks, PPT/PPTX/website artifact handling placeholders.
9. Templates/characters skeleton.
10. Community skeleton.
11. Games + IM integration boundary skeleton.
12. News skeleton.
13. Billing/subscription/credits skeleton with Play Billing boundary and backend verify hooks.
14. File upload pipeline: presigned URL → PUT upload → process/complete endpoint.
15. Push boundary: FCM service skeleton, token registration endpoint.
16. Local persistence: Room/DataStore as documented.
17. Teen/content filter settings with local DB skeleton.
18. Release-ready project structure with product flavors dev/test/preview/prod.
19. Backend-sidecar implementation plan for Agnes gateway proxy and app API surface.
20. Tests/build validation plan.

TECH STACK REQUIREMENTS
Use modern Android/Kotlin stack:
- Kotlin
- Gradle Kotlin DSL
- Android Gradle Plugin compatible with compileSdk 36
- Jetpack Compose for chat blocks and modern screens
- Navigation Compose or clear single-activity navigation architecture
- Coroutines + Flow
- Retrofit2 + OkHttp3 + Gson or kotlinx.serialization; prefer consistency with docs: Retrofit2 + OkHttp3 + Gson
- OkHttp SSE/streaming support
- Room for local DBs
- DataStore Preferences for lightweight caches/settings
- Media3 for voice/playback boundary
- Credential Manager / Google sign-in boundary as placeholder/configurable
- Play Billing KTX boundary as placeholder/configurable
- Firebase Messaging boundary as placeholder/configurable
- Optional SDK boundaries for Tencent IM, Singular, Adjust, AppsFlyer as interfaces/stubs behind build flags unless real configs are provided

BACKEND-SIDECAR REQUIREMENTS
Plan a backend-sidecar that owns all direct calls to Agnes Gateway:
- Reads `AGNES_API_KEY`, `AGNES_BASE_URL`, `AGNES_POLL_BASE_URL` from env/secret manager.
- Uses app-facing endpoints from `README.md` and `docs/01-api-surface.md`.
- Maps app `model_code` to provider models via `backend-sidecar/model-routing.yaml`.
- Implements at least these gateway mappings:
  - `POST /api/v1/agnes/chat/stream` → `POST /v1/chat/completions`, stream true, model usually `agnes-2.5-flash`
  - `POST /api/v1/agnes/chat/stream/regenerate` → reissue last user turn
  - `POST /api/v1/agnes/conversation/title-summary` → `agnes-1.5-flash`
  - `GET /api/v1/agnes/follow-up-questions` → `agnes-1.5-flash`
  - `POST /api/v1/agnes/image_ocr` → multimodal `agnes-2.5-flash`
  - image studio/edit → `/v1/images/generations`, default `agnes-image-2.1-flash`
  - video optional → `/v1/videos` + `/agnesapi?video_id=...`
- Transforms OpenAI/Agnes SSE into app stream blocks.
- Enforces quota/plan gates before gateway calls.
- Applies retry/backoff only for statuses listed in `docs/04-ai-gateway.md`.
- Logs only sanitized telemetry; never secrets or raw private payloads by default.

AGNES GATEWAY MODELS TO RESPECT
Provider models:
- `agnes-2.5-flash`: default chat/agents/tools/vision, 512K context, 65.5K max output.
- `agnes-2.0-flash`: legacy/fallback, 256K/64K.
- `agnes-1.5-flash`: fast short tasks like titles/follow-ups.
- `agnes-image-2.1-flash`: default image generation/editing/multi-image.
- `agnes-image-2.0-flash`: fast image fallback.
- `agnes-video-v2.0`: async video.

Image request rules:
- Use size tiers `1K`, `2K`, `3K`, `4K`.
- Use `ratio`, e.g. `1:1`, `16:9`, `9:16`, `3:4`, `4:3`, `21:9`.
- Put `response_format` under `extra_body.response_format`.
- For edit/multi-image, use `image: string[]` with public URLs or Data-URI Base64.
- Do not use obsolete `tags: ["img2img"]`.

Video request rules:
- Create via `POST {AGNES_BASE_URL}/videos`.
- Poll via `GET {AGNES_POLL_BASE_URL}/agnesapi?video_id=<VIDEO_ID>`.
- Use `video_id`, not `task_id` for current polling.
- `num_frames <= 441` and follows `8n + 1`.
- Poll around every 5 seconds, not tight loops.

APP API SURFACE TO MODEL
Use `README.md` §5 and `docs/01-api-surface.md` as authoritative. Generate a plan for Retrofit service interfaces and DTOs for:
- AuthApi
- ChatApi
- ArtifactApi
- FileApi
- TemplateApi
- GameApi
- NewsApi
- BillingApi
- InvitationApi
- CommunityApi
- SysApi

The app backend response envelope is:
- `BaseResponse<T> { code, message, data }`
- `BaseNoResponse`
- Pagination via `Pagination` / `PaginationInfo`.

ANDROID ARCHITECTURE TO PLAN
Propose a clean, implementable module/package structure aligned to repo docs:
- `core.network`: Retrofit, OkHttp, interceptors, ApiResult adapter, SSE parser
- `core.model`: BaseResponse, pagination, common DTOs
- `core.datastore`: DataStore keys/cache
- `core.database`: Room DB wiring
- `core.ui`: theme/design components
- `feature.auth`
- `feature.chat`
- `feature.artifacts`
- `feature.upload`
- `feature.image_studio`
- `feature.templates`
- `feature.community`
- `feature.games`
- `feature.news`
- `feature.billing`
- `feature.profile`
- `feature.filters`
- `feature.push`
- `feature.webview`
- `app`: MainActivity, navigation, dependency wiring

You may keep a single Android Gradle module initially if simpler, but plan packages so they can be split later.

AUTH / SECURITY REQUIREMENTS
- Store access/refresh tokens securely. Prefer EncryptedSharedPreferences or DataStore + Android Keystore strategy; explain choice.
- Add OkHttp auth interceptor.
- Add refresh-token flow using `/api/v1/user/refresh-token`.
- Add logout cleanup and FCM token cleanup hooks.
- Do not connect to original production backends without authorization. Use configurable `BASE_URL` by flavor.

ENVIRONMENTS / FLAVORS
Plan product flavors:
- dev
- test
- preview
- prod
Each injects at least:
- `BASE_URL`
- `H5_URL`
- feature flags for Firebase/Tencent/Attribution/Billing if configs are missing
Use placeholder URLs unless user provides real owned backend URLs.

STREAMING CHAT REQUIREMENTS
Plan and later implement:
- `ChatStreamRequestBody` with fields documented in repo:
  `conversation_id`, `message`, `tool_mode`, `scene`, `agent_type`, `attachments`, `model_code`.
- SSE parser that can handle partial chunks and reconnection/resume.
- Stream event/block model sealed classes:
  `ThinkingBlock`, `SkillLoadBlock`, `ToolCallBlock`, `TextBlock`, `ImageBlock`, `ArtifactBlock`, `FollowupsBlock`, `ErrorBlock`, `DoneBlock`.
- UI renderer in Compose for each block type.
- Thinking accordion state backed by DataStore/remembered state similar to `thinkingExpandMap`.
- Cancel, resume, regenerate and HITL-resume actions.

UPLOAD REQUIREMENTS
Plan file upload pipeline:
1. Request presigned URL.
2. PUT bytes to storage URL.
3. Call process/complete endpoint.
4. Return asset URL/id for chat attachment or image edit reference.
Support avatar and chat attachments separately.

LOCAL PERSISTENCE REQUIREMENTS
Plan:
- Room DB for content filters / teen mode (`AgnesFilterDatabase` equivalent).
- Room DB for photo picker cache if needed.
- DataStore for:
  - auth/session non-secret flags
  - chat UI prefs such as thinking expansion
  - community/game/credits caches
  - feature flags
  - teen mode state metadata, not raw PIN if avoidable

BILLING REQUIREMENTS
Plan Play Billing boundary and backend verification endpoints:
- `GET /api/v2/subscription/plans`
- `GET /api/v2/subscription/credits-balance`
- `GET /api/v1/subscription/credits-packs`
- `POST /api/v1/subscription/credits-transactions`
- `POST /api/v1/subscription/verify-payment`
- `POST /v2/purchase`
- `POST /api/v1/subscription/cancel`
If real Play products are not configured, use stubs/mocks behind an interface.

PUSH / IM / ATTRIBUTION REQUIREMENTS
Plan boundaries, not hard dependencies unless configs exist:
- FCM service and `/api/v1/fcm/token` registration.
- Tencent IM SDK integration boundary via interface; backend endpoint `/api/v1/im/user-sig`.
- Singular/Adjust/AppsFlyer as optional attribution adapters disabled by default.

LEGAL / SAFETY REQUIREMENTS
- Do not copy proprietary source code or assets.
- Do not use original production domains/brand in a shipped rebuild without permission.
- Keep package/app branding configurable.
- Replace original Firebase/Tencent/Attribution configs with owner-provided projects.

WHAT TO OUTPUT IN PLAN MODE
Produce a detailed plan with these sections:

1. Repository assessment
   - What currently exists
   - What is missing
   - Whether build currently succeeds or why it will not

2. Proposed final architecture
   - Android architecture diagram in text
   - Backend-sidecar architecture diagram in text
   - Data flow for chat/image/video/upload/billing

3. Gradle/build plan
   - Plugins and versions
   - Dependencies
   - Flavor/buildConfig plan
   - Required manifest components/permissions

4. Package/file plan
   - Exact package structure
   - Major Kotlin files/classes/interfaces to create
   - DTO/service list

5. Backend-sidecar plan
   - Env/secrets
   - Gateway clients
   - Model routing
   - SSE transformation
   - Quota/error/retry handling

6. Feature implementation phases
   Phase 0: build foundation
   Phase 1: core network/auth/session
   Phase 2: chat streaming and block renderer
   Phase 3: upload/image/artifacts
   Phase 4: profile/settings/filters/teen mode
   Phase 5: billing/subscriptions/credits
   Phase 6: community/games/news/templates skeletons
   Phase 7: push/IM/attribution boundaries
   Phase 8: tests/release hardening

7. Acceptance criteria per phase
   - Build commands
   - Unit tests
   - UI smoke tests
   - Mock backend tests
   - Manual QA checklist

8. Risks and assumptions
   - Missing real backend
   - Missing Firebase/Tencent/Billing configs
   - Agnes gateway key must be provided only server-side
   - Original APIs may not be accessible/authorized

9. ACT MODE execution proposal
   - Give an ordered list of the first file changes you would make once ACT MODE is approved.
   - Prefer a minimal compileable vertical slice first: app launches → mock login/session → chat screen → mock SSE stream renders blocks.

PLANNING QUALITY BAR
- Be concrete and file-level, not generic.
- Make it possible to implement incrementally and keep the app building after each phase.
- Prefer mocked interfaces where real external services are not configured.
- Clearly separate Android client work from backend-sidecar work.
- Keep the gateway API key server-side only.
- Explicitly use the updated `docs/04-ai-gateway.md` and `backend-sidecar/model-routing.yaml`.

BEGIN BY INSPECTING THESE FILES
- `settings.gradle.kts`
- `build.gradle.kts`
- `app/build.gradle.kts`
- `README.md`
- `docs/01-api-surface.md`
- `docs/02-screens.md`
- `docs/03-release-checklist.md`
- `docs/04-ai-gateway.md`
- `docs/05-agent-skills.md`
- `docs/06-app-configuration.md`
- `backend-sidecar/README.md`
- `backend-sidecar/.env.example`
- `backend-sidecar/model-routing.yaml`
- `backend-sidecar/src/main/kotlin/com/agnes/sidecar/AgnesGatewayContracts.kt`
- `TODO-8-UMSETZUNG.md`

Now produce the full implementation plan. Do not modify files until ACT MODE is approved.
```
