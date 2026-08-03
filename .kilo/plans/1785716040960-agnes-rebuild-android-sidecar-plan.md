# Agnes-Style Android Rebuild — Implementation Plan (v3.0.61)

Target: turn the `agnes-rebuild` spec repo into a buildable Android app (Compose, single-activity,
MOCK-first) plus a `:sidecar` Kotlin/Ktor backend that proxies the Agnes AI gateway. All gateway
secrets live server-side only.

Sections: 1 assessment · 2 architecture · 3 Gradle/build · 4 package/file plan · 5 sidecar plan ·
6 phases · 7 acceptance criteria · 8 risks/assumptions · 9 ACT-MODE first changes.

---

## 1. Repository assessment

### 1.1 What exists

| Artifact | State |
|---|---|
| `README.md` | Authoritative spec: §5 endpoint map (90+ bindings), §7 agent/skill runtime + stream-block protocol, §8 gateway, §12 acceptance checklist |
| `docs/01-api-surface.md` | Retrofit interface grouping (`AuthApi`…`SysApi`) + SSE block contract |
| `docs/02-screens.md` | Single-activity nav, screens, `AgnesBridge` JS facade |
| `docs/03-release-checklist.md` | Flavor/BuildConfig, deps list, manifest/permissions, AAB splits |
| `docs/04-ai-gateway.md` | **Normative** gateway contract: routes, model table, quota/RPM tables, error matrix, retry rules, SSE proxy contract |
| `docs/05-agent-skills.md` + `SKILLS-RECHERCHE.md` | Skill model, `ToolCallEnum`, Edit-Image chain, skill-store format |
| `app/build.gradle.kts` | namespace `com.agnes.bundle_agnes`, appId `com.sobrr.agnes`, minSdk 26, compileSdk **34**, target **34**, versionCode 3000061, versionName 3.0.61, `buildFeatures.compose = true`, **no dependencies, no manifest, no res** |
| `app/src/main/java/**` | **449 empty Java stub files** (markers mirroring original structure): `com.agnes.bundle_agnes.{db,ui.chat(~52 comps),ui.game}`, `com.agnes.feature_{billing(~23),community,game,task(ToolCallEnum,ChatStreamRequestBody,…),push,photo_picker}`, `com.agnes.upload`, `com.sobrr.agnes.data.{model(→ `BaseResponse`/`BaseNoResponse`/pixa/envelope),network(ApiResult…)}`, `com.sobrr.agnes.feature_{auth,filters}` |
| Root build / settings | `build.gradle.kts` (AGP 8.7.0, Kotlin 2.0.21, compose plugin), `settings.gradle.kts` includes only `:app` |
| Git | Branch `build`+`master`; `README` §13 legal notice; no original code/assets/strings |

### 1.2 What is missing (gaps to create)

1. `AndroidManifest.xml` + `res/` + `proguard-rules.pro` — **no manifest → AGP build fails today**.
2. Gradle wrapper (`gradlew`, `gradle/wrapper/*`), `gradle/libs.versions.toml`, module `:sidecar`.
3. All real Kotlin sources (current files are non-compiling markers — a single stub `buildFeatures.compose` alone doesn't count).
4. Tests: `test/` + `androidTest/` (JUnit5/Turbine/MockWebServer), contract fixtures.
5. Backend sidecar entirely: `backend-sidecar/` (`.env.example`, `model-routing.yaml`, `src/main/kotlin/…/AgnesGatewayContracts.kt`) — **referenced by the task but absent from this repo**.
6. Referenced docs absent here and treated as external: `docs/06-app-configuration.md`, `ANALYSE-EINSTELLUNGEN.md`, `TODO-8-UMSETZUNG.md`. Do not block on them; mirror only README/docs-01–05.

### 1.3 Does it build today?

**No.** Failures, in order: (a) no wrapper/`gradlew`, AGP download requires internet; (b) missing `AndroidManifest.xml` → AGP abort; (c) `app/build.gradle.kts` declares Kotlin plugins but zero Kotlin sources and zero `dependencies {}` blocks; (d) `compileSdk` 34 vs required 36; (e) the 449 `.java` markers are valid classes but reference nothing, so they do not satisfy the manifest or app.

### 1.4 Key decisions (RESOLVED with user — bind these; do not reopen)

- **Identity/brand**: keep spec values `applicationId = com.sobrr.agnes`, namespace `com.agnes.bundle_agnes`, per-flavor configurable. Dev-only rebuild; never point at original production domains. (User rejected v2's `com.example.companion`; repo spec values win.)
- **compileSdk/targetSdk 36, min 26**; AGP 8.11.x (≥8.9.1 required).
- **DI**: manual `AppContainer` (user-confirmed, no Hilt); repositories bound by `BuildConfig.API_PROFILE`. No KSP beyond Room's.
- **MOCK default / LIVE optional**: user-confirmed runtime model:
  - `API_PROFILE=MOCK` (default, any flavor unless overridden): **in-app in-memory fake repositories**; chat stream is a scripted `MockSseSource` Flow emitting the 9 block types (mock login/session, mock conversations, edit-image chain replay). Zero HTTP on this path, no gateway key.
  - `API_PROFILE=LIVE` + flavor base URLs: Retrofit → **sidecar** (`test` flavor default `http://10.0.2.2:8080` for emulator → Ktor). No embedded app server; the sidecar itself runs with its own `PROFILE=mock|live`.
- **Token storage**: DataStore(preferences) + Android Keystore wrapped AES-GCM envelope (`androidx.security:security-crypto` deprecated; custom Keystore strategy). Never raw tokens on disk.
- **UI**: full Compose M3 single-activity; stream blocks as Compose components (mapped from the 52 placeholder markers).
- **Tests**: JUnit4 + Turbine + MockWebServer (unit/boards); sidecar contract tests are MockWebServer-based, fixtures recorded, never a live key.

## 2. Final architecture

### 2.1 Android (single process, single activity)

```
Android app (com.agnes.bundle_agnes + com.sobrr.agnes)
│  MainActivity (Compose, Navigation-Compose graph)
│  AgnesApplication (AppContainer init)
│
├─ core.model      → BaseResponse<T> / BaseNoResponse / Pagination(+Info) / shared DTOs (from stubs)
├─ core.network    → Retrofit(OkHttp+Gson), ApiResultAdapter, AuthInterceptor+RefreshFlow, SseClient
│                     BaseUrl per flavor, API_PROFILE switch; OkHttp-SSE source consumer
├─ core.datastore  → AgnessPrefs: auth flags, thinkingExpandMap, feature flags, caches
├─ core.database   → Room: AgnesFilterDatabase · PhotoPickerDatabase (no stored PIN if avoidable)
├─ core.ui         → M3 theme, DesignSystem blocks, shared composables
│
├─ feature.auth    → login/register(bind)/refresh/logout; token store (Keystore+DataStore)
├─ feature.chat    → ConversationRepo + ChatViewModel + SSE state machine + block renderers
├─ feature.artifacts / feature.upload (presigned→PUT→process) / feature.image_studio
├─ feature.templates · community · games · news · billing (PlayStub + verify hooks)
├─ feature.profile · filters (Room) · push (FcmService + token register) · webview (AgnesBridge)
└─ app             → MainActivity, nav graph, AppContainer wiring
```

### 2.2 Backend-sidecar architecture (`backend-sidecar/`, Kotlin Ktor)

```
Android app ── HTTPS BASE_URL (envelope BaseResponse, SSE /chat/stream) ──► :sidecar (Ktor Netty)
   :sidecar:  mod_route (env+secret) → mod_client (AgnesGateway OpenAI-compatible)
              mod_routing (model-routing.yaml: app model_code ↔ gateway model)
              mod_sse (gateway delta → app block protocol: thinking/skill_load/tool_call/…/done)
              mod_agent (skill store: SKILL.md dirs, agent loop emits tool_call blocks)
              mod_quota (RPM/plan ledger; 402→upgrade-gate envelope)
              mod_store (MOCK in-memory harness; LIVE switches to real gateway)
   (MOCK profile: no network to gateway; scripted SSE fixtures; LIVE: AGNES_API_KEY from env only)
   QoS: retry/backoff 408/429/500/502/503/504/520/522/524; sanitized logs; /healthz
```

### 2.3 Key data flows

- **Chat**: app `ChatStreamRequestBody{conversation_id,message,tool_mode,scene,agent_type,attachments,model_code}` → `POST /api/v1/agnes/chat/stream` → sidecar: quota → gateway `chat/completions stream=true` → SSE deltas → block tokens (`thinking…done`) → app SSE chunker rebuilds partial chunks → `ChatViewModel` → Compose renderers. Actions: `cancel`, `resume` (event-id), `regenerate` (reissue last turn), `hitl-resume` (with HITL payload).
- **Image studio**: `text→image`→`POST /api/v1/agnes/…image…` 2-step; edit-image: upload→presigned→PUT→process→asset URL → app sends `tool_mode="text_edit_image"` → sidecar skill chain `LoadSkill(image-generation)→LoadSkill(reference-image)→LoadSkill(image-prompt-craft)→GenerateImage(reference=url)`; gateway `images/generations`, `response_format` under `extra_body.response_format`, size tiers 1K/2K/3K/4K, ratio strings, `image:string[]` for edit.
- **Video** (optional): app/uploading target → `POST {AGNES_BASE_URL}/videos`; poll `GET {POLL_BASE}/agnesapi?video_id=…` every ~5s; `num_frames≤441` & `8n+1`.
- **Upload**: `POST presigned-url` → `PUT` bytes → `POST /api/process/{chat|avatar|…}` → asset id/url (avatar and chat attachments).
- **Billing (stub boundary)**: app purchases→(BillingProxy stub/MOCK)→backend `GET /subscription/plans`, `POST /subscription/verify-payment`/`/v2/purchase`, credits ledger endpoints; real Play products behind interface only.

---

## 3. Gradle / build plan

### 3.1 Settings & root

- `settings.gradle.kts`: `pluginManagement { repositories { google(), mavenCentral(), gradlePluginPortal() } }`, `dependencyResolutionManagement`, `include(":app")`, `include(":sidecar")`.
- `gradle/libs.versions.toml` — locked versions (bump allowed if resolution fails, doc the bump):

| Component | Version |
|---|---|
| Kotlin | 2.1.21 |
| AGP | 8.11.1 (compileSdk 36 requires ≥8.9.1) |
| Compose BOM | 2025.06.01 (or latest resolved) |
| ksp | 2.1.21-2.0.1 |
| Room | 2.7.1 |
| Retrofit2 / OkHttp3 | 2.11.0 / 4.12.0 (okhttp3-sse incl.) |
| Gson | 2.11.0 |
| Coil | 2.7.0 (rememberAsyncImagePainter for image blocks) |
| Navigation Compose | 2.9.0 |
| Media3 | 1.5.1 (boundary only) |
| DataStore | 1.1.7 |
| Play Billing KTX | 7.1.0 (stub interface) |
| Firebase BOM | 33.7.0 (feature-push only; optional) |
| Tencent IM / attribution | OMITTED from deps; interfaces + config stubs only |
| Test | JUnit4 4.13.2, turbine 1.2 (now `app.cash.turbine:turbine:1.2.0`), MockWebServer 4.12.0, coroutine-test 1.10.x, Espresso 3.6.1 |
| Sidecar (JVM) | Kotlin 2.1.21, Ktor 3.0.x, kotlinx-serialization 1.7.3, logback |

### 3.2 `app/build.gradle.kts`

- `namespace = "com.agnes.bundle_agnes"`, `applicationId = "com.sobrr.agnes"`, min 26, compile 36, target 36, versions above.
- `java buildFeatures { compose=true; buildConfig=true }`, `composeOptions` none (Kotlin 2 compose plugin), `packaging resources`.
- **Flavors** `dev|test|preview|prod` each defining BuildConfig fields: `BASE_URL`, `H5_URL`, `API_PROFILE` (default `MOCK`; `test` may force `LIVE`), `ENABLE_FIREBASE=false`(prod=true), `ENABLE_TENCENT_IM=false`, `ENABLE_ATTRIBUTION=false`, `ENABLE_BILLING=false`, `ENABLE_PUSH` (mirror of FCM), manifestPlaceholders `fcm_default_channel`.
- Flavor default base URLs (placeholders, **never** original prod domains): `dev` → in-app MOCK only (`BASE_URL` unused unless LIVE), `test` → `http://10.0.2.2:8080` (emulator → sidecar), `preview`/`prod` → own placeholder host (documented).
- `dependencies` per plan + flavor-specific `debugImplementation`.
- R8 enabled release; AAB with splits: abi-arm64-v8a, language `en`, density `hdpi`.
- Provide placeholders: `google-services.json` → a `.json` with placeholder `project_id` + doc; SDK init gated by `ENABLE_*` flags.

### 3.3 Manifest / permissions

Components: `AgnesApplication`, `MainActivity` (singleTask, exported launcher), `com.agnes.feature_push.service.FcmService` (FirebaseMessagingService **stub**, wired to `ENABLE_PUSH`), `FileProvider`, Android 12 splash (via theme) optional.

Permissions: INTERNET, ACCESS_NETWORK_STATE, POST_NOTIFICATIONS, VIBRATE, WAKE_LOCK, CAMERA (optional), RECORD_AUDIO (optional), READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_EXTERNAL_STORAGE (maxSdk 32), AD_ID (opt-in, only if attribution enabled), BILLING (via Play, no manifest permission), C2DM/RECEIVE (push only, flag).

---

## 4. Package / file plan (single `:app` module, packages split-ready)

Final root packages ─ after the marker layout:

- `com.agnes.bundle_agnes`: `AgnesApplication`, `MainActivity`, `core/AppContainer.kt`, `core/Navigation.kt`, `ui/theme/*`, `ui/chat/**` (placeholder markers become Kotlin components).
- `com.sobrr.agnes`: `data/model/*` (base envelope, media, pushData, pixa…), `data/network/*` (the rest ported from existing stubs to Kotlin), `feature_auth/**`, `feature_filters/**`.
- `com.agnes.feature_*`: new surfaces under existing packages (billing/community/game/task/upload/push/photo_picker).

### 4.1 `:sidecar` layout (new)

```
backend-sidecar/
├ .env.example            AGNES_API_KEY=, AGNES_BASE_URL=https://apihub.agnes-ai.com/v1,
│                         AGNES_POLL_BASE_URL=https://apihub.agnes-ai.com, PROFILE=mock|live, PORT=8080
├ model-routing.yaml      routes below §5.3
├ build.gradle.kts        Ktor application
└ src/main/kotlin/com/agnes/sidecar/
    AgnesGatewayContracts.kt   (OpenAI-compatible DTOs, gateway interface, SSE types)
    Routes.kt, Healthz.kt
    gateway/{AgnesGatewayClient.kt, SseConverter.kt, ImageClient.kt, VideoPoller.kt}
    routing/{ModelRegistry.kt, ModelRoutingYaml.kt}
    agent/{SkillStore.kt, AgentLoop.kt, Skills/{image-generation/SKILL.md+reference-image.md, image-prompt-craft, slides, web-search}}
    quota/{QuotaLedger.kt, QuotaRateLimiter.kt, UpgradeGateCode.kt}
    store/{MockStore.kt (users, conversations, artifacts, images, credits)}
    envelope/{ErrorMapper.kt}            # 400/401/… → BaseResponse/UX mapping
    test/  # MockWebServer contract tests, retry/route/quota tests
```

### 4.2 DTO/service list (all names taken from README/docs stubs)

`core/model`: `BaseResponse`, `BaseNoResponse`, `Pagination`, `PaginationInfo`, enums (`ToolCallEnum`, `ToolCallType`, `StreamBlockType`), media + pixa payloads.

`AuthApi`: `AuthEmail/AuthPhone/AuthGoogle`, `RegisterByEmailRequest`, `BindEmail`, `BindPhone`, `AuthToken`, `Me`, `OwnerProfileResponse`, refresh, code/send·verify, fcm/token, timezone, account-delete.

`ChatApi`: `ChatStreamRequestBody`, `ChatRegenerateRequestBody`, `ChatResumeStreamRequestBody`, `ChatHitlResumeRequestBody`, `Conversation{List,Detail,Create,Update,History,SearchRes,Status,Title}`, `AgnesMultiConversationList`, `FollowUpQuestions`, `ModeSupportModelsResponse`, `AgnesImageOcrRequest`, `PptUpgradeGateCheckResponse`, `Topics`, `TtsToggleRequest`.

Stream model (app side `feature.chat.model`): sealed `StreamBlock` hierarchy:
`ThinkingBlock·SkillLoadBlock·ToolCallBlock(type, tool_call_id, payload)·TextBlock·ImageBlock·ArtifactBlock·ErrorBlock·DoneBlock·FollowupsBlock`; `StreamBlockType` enum; persisted `ThinkingExpandMap` via DataStore.

Tool card classes: `ToolCallCard` composables for LoadSkill, GenerateImage, WebSearch, ImageSearch, ReadFile, WriteFile, EditFile, ListFiles, Execute, WriteReport, QueryWeather, ProfileData, Other.

`ArtifactApi`: `GetArtifactShareReply`, `ShareArtifactSlide{Page,Sheet,Html,Report,Media}`, `ImChatReloadDiskPayload`, `ConversationSandbox/Uploads`.

`File/upload`: `PresignedUrl(Dto,Response)`, `MultipartInit/Complete/Abort`, `AssetItem`, `DeleteAssetsRequest`.

`Template`: `CreateRoleDraft`, `CharacterItem/Detail`, `GenerateTemplateResponse`, `Pagination`.

`Game`: `GameList/Detail/GameInfoResponse`, `GameCategoryTree`, `GameProfile`, `GameGroupsModel`, `GameHelpAnswerRequest`, `JoinGameByShareCodeRequest`.

`News`: feed/DTO payload per README §5.7.

`Billing (stub, Play products absent)`: `GooglePlan`, `PlanPrice`, `PricingPhase`, `CurrentSubscription`, `CreditsPacksBean`, `CreditsFuel/GoogleFuelPack`, `PointsTransaction`, `CancelSubscription`, `GooglePayRequest`.

`Invitation/Community`: per README §5.9/§5.10.

`Sys`: `FeatureConfig`, `GlobalErrorConfig`, `MigrationWaitingResponse`.

### 4.3 Count estimate
~180–230 Kotlin files in `:app` (sources + tests), ~45 in `:sidecar` (incl. tests). Exact names driven by spec; stub markers provide inheritance map.

---

## 5. Backend-sidecar plan (create `backend-sidecar/` from scratch)

- **Secrets**: only `AGNES_API_KEY` (server env/secret manager); `AGNES_BASE_URL` (default `https://apihub.agnes-ai.com/v1`, alternate `.cn`, China `api.agnes-ai.cn/v1`), `AGNES_POLL_BASE_URL` (`https://apihub.agnes-ai.com`). App-facing API is envelope `BaseResponse<T>` + SSE stream.
- **Gateway clients** (Ktor/Retrofit in sidecar):
  - `POST /v1/chat/completions` (openai-compliant, `stream`), `POST /v1/images/generations` (`extra_body.response_format`, size tiers/ratio, `image: string[]` for edits), `POST /v1/videos` + `GET /agnesapi?video_id=` (never legacy `task_id`; `num_frames ≤441`, `8n+1`; poll ~5s).
  - Beware: don't double-append `/v1` (base already `/v1`; image/video poll off `/v1`).
- **Model routing** (`model-routing.yaml`, authoritative map - mirrored here):
  - `chat/stream`, regenerate, HITL, tools/vision → `agnes-2.5-flash`
  - title-summary, follow-ups → `agnes-1.5-flash`
  - persona/opening-line, game help → `agnes-2.0-flash`
  - image OCR / vision tasks → `agnes-2.5-flash` (image_url input)
  - image gen/edit → `agnes-image-2.1-flash` (fallback `agnes-image-2.0-flash`)
  - video → `agnes-video-v2.0` (poll by `video_id`, never legacy `task_id`)
- **SSE transform**: parse gateway `data:` deltas → assemble app blocks protocol (thinking/skill_load/tool_call/text/image/artifact/followups/error/done); re-emit as `event:block\ndata:{json}\n\n` lines to app; handle partial chunk/pipeline and store last event id for resume.
- **Quota**: `QuotaLedger` meters text requests, image count (1K/2K/3K/4K → RPM from docs §6.2) & video seconds(§6.3 labels); `402`/quota-exhausted → upgrade-gate envelope (`PptUpgradeGate`-style). Surface balance via `/credits-*`.
- **Error/retry**: exponential backoff with jitter only for `408,429,500,502,503,504,520,522,524`; `400/401/403/422` logic; map via error matrix §6.4 to UX codes.
- **Logging**: sanitized telemetry only (model, path, code, latency; no key/no message bodies).
- **MOCK mode**: sidecar serves all README §5 endpoints from in-memory store + SSE fixture scripts, incl. the edit-image chain replay — enables integration tests without a key.

---

## 6. Implementation phases (each Phase compiles + smoke-green)

**Phase 0 — Build foundation**
- gradle wrapper (`gradle wrapper`), `settings.gradle.kts` incl `:sidecar`, catalog `libs.versions.toml`, root/`build.gradle.kts`, `app/build.gradle.kts` flavors+BuildConfig, minimal `res/` (theme, strings, colors), placeholder `AndroidManifest.xml`, `proguard-rules.pro`, `.gitignore`(*), `gradle.properties`.
- Gate: `./gradlew :app:assembleDebug` green.

**Phase 1 — Core network / auth / session**
- `data/model` envelope + Gson adapters; `retrofit` + `ApiResultAdapter`; `AuthService` via sidecar; `AuthInterceptor` + `RefreshInterceptor` (loop-safe / retry-once); Keystore AES-GCM token store + `SessionManager`; refresh `/api/v1/user/refresh-token`; logout cleanup (incl. `clear_firebase_token`).
- `AppContainer` DI; splash → auth → shell nav.
- MOCK repos (`MockAuthRepository`) bound on `API_PROFILE=MOCK`; `LoginScreen`/`SignUpScreen` cover email/phone/Google login + bind flows (UI) — all providers behind stub implementations.
- Gate: `./gradlew :app:testDebugUnitTest` + assembleDebug; manual login via MOCK.

**Phase 2 — Chat streaming + block renderer**
- SSE client (OkHttp SSE / dedicated parser w/ chunk assembly + resume cursor), `ChatViewModel` state machine (connecting/streaming/cancelled/error/resumed), actions: send/regenerate/cancel/resume/hitl.
- Compose: conversation list (search, running badge), conversation screen: accumulator for blocks, renderer for each block type; thinking accordion persisted via `thinkingExpandMap` DataStore.
- Sidecar: SSE fixture + MOCK stream endpoints; Edit-Image replay fixture.
- Tests: unit for SseParser + ViewModel (Turbine); UI replay of mock stream.

**Phase 3 — Upload/image/artifacts**
- Upload pipeline (`presigned→PUT→process/complete`); avatar + chat attachments; AIGC assets; image studio (T2I, edit-image via `tool_mode="text_edit_image"` + reference upload; prompt-craft hook); visuals/materials; OCR; watermark removal stub; artifacts list/download/share; PPT page viewer (placeholder slide pages + pptx download); website publish. Tests: MockWebServer sequence streams.

**Phase 4 — Profile/settings/filters/teen mode**
- Profile v2 (`/api/v2/user/profile`), name/avatar/timezone; settings; Room `AgnesFilterDatabase` (filters); teen-mode metadata persisted in DataStore (no raw PIN); content-filter network bindings; `PhotoPickerDatabase`.

**Phase 5 — Billing/subscriptions/credits**
- `BillingRepository` interface + `PlayBillingStub`/Mock impl; plans/credits-balance/credits-packs/credits-transactions on app backend; purchase verify v1 + v2 (`/v2/purchase`); cancel; upgrade-gates (`PptUpgradeGateCheck`); quotas surfaced from `mode_support_models`.

**Phase 6 — Community/games/news/templates skeletons**
- Feed CRUD, emoji reacts, comments; game catalog/tree/detail/groups/share-code; news feed/prefs/read-stats; template gallery (list/like/collect/dislike/drafts). Repos from README §5.x against sidecar MOCK; basic UI lists.

**Phase 7 — Push/IM/attribution boundaries**
- `FcmService` stub (+ FCM token→`/api/v1/fcm/token`); `ImLogin` interface + `MockImLogin` + optional Tencent IM adapter behind `ENABLE_TENCENT_IM`; `Singular`/`Adjust`/`AppsFlyer` adapters behind `ENABLE_ATTRIBUTION` (default off, no SDK deps until configured); WebView shell + `AgnesBridge` (token, nav, picker, upload, billing, share).

**Phase 8 — Tests/release hardening**
- Contract tests MockBackend (sidecar) + `MockWebServer` app; UI smoke (Compose test screenshot for stream); unit coverage; R8; AAB splits (armv64-v8a/en/hdpi); flavor `prod` checklist per `docs/03`; README §12 acceptance checklist matrix.

---

## 7. Acceptance criteria (per phase)

| Phase | Build | Unit tests | Mock-backend tests | Manual QA |
|---|---|---|---|---|
| 0 | `./gradlew :app:assembleDebug` | — | — | app opens placeholder app |
| 1 | same + `:app:testDebugUnitTest` | envelope/ApiResult/Auth↔refresh loop | sidecar `/api/v1/user/*` MOCK | login/logout/refresh flow |
| 2 | same | SseParser, ChatViewModel state machine (Turbine) | SSE fixtures replay incl. edit-image | stream renders all 9 blocks; thinking toggle; cancel/regenerate/resume/hitl survive process death |
| 3 | same | UploadExecutor, artifact DTO | presigned 2-step mock | upload→chat attach; T2I + edit-image; artifact list/download |
| 4 | same | filter DB (Room) + teen-flag logic | /filters endpoints | set filters; teen mode block/restore |
| 5 | same | BillingStub + verify mapping | `/subscription/*`,`/purchase` MOCK | plans, credits balance, mock purchase/fail back, upgrade gate |
| 6 | same | feature repos vs DTO | `/template /game /news /community` MOCK | community flow, games list, news feed |
| 7 | same | bridge + token lifecycle | `/im/user-sig` + `/fcm/token` MOCK | push toggles, bridge nav |
| 8 | `./gradlew :app:assembleRelease` (AAB splits) · `:sidecar:test` | full unit suite | contract tests (recorded fixtures, no live key) | acceptance checklist §12 |

Sidecar gates: `./gradlew :sidecar:test` green each phase; container·`/healthz`.

## 8. Risks & assumptions

1. **No real backend**: default MOCK everywhere; LIVE documented how to flip via env + base URL. No original prod domains (placeholders).
2. **No Firebase/Tencent/Billing/pub-key**: gated behind `ENABLE_*`, doc‑first; no SDK keys in repo.
3. **`AGNES_API_KEY`** only in sidecar env/.env.example (placeholder), never in app. No real keys anywhere.
4. AGP 8.11.1 vs 8.7 pinned in root — bump if a build tool mismatch; document.
5. `compileSdk 36` requires SDK download in CI; if offline, fallback to 35 with AGP compatible (documented deviation).
6. Jetpack `security-crypto` deprecated → use Keystore AES-GCM; test on emulator.
7. `backend-sidecar/`, `docs/06`, `TODO-8-UMSETZUNG`, `ANALYSE-EINSTELLUNGEN` absent today — created fresh from README/docs 01–05; no external doc to reconcile.
8. Original tool-chain specifics (`slides`, PPTX) can't be fully replicated without AWS/SDK resources; provide preview+download only browser-based where feasible (mark "placeholder").
9. Navigation/nav graph: single-activity Navigation Compose (valid even for hybrid H5 wrappers).
10. DTO names are reconstructed identifiers from spec docs; enforce @SerializedName matching to keep envelope stable.
11. **Environment limitation**: The current development environment has Java 11 only, but AGP 8.x requires Java 17. This prevents local builds. Workaround: use a CI environment with Java 17+, or downgrade to AGP 7.x with Java 11 (loses compileSdk 34+ support). The plan assumes Java 17+ in CI/CD.

---

## 9. ACT-MODE first changes (vertical slice order)

Build the slice with `API_PROFILE=MOCK` (in-app fakes, zero network) so every sub-step is runnable on a plain emulator; the Ktor sidecar is only wired in at the end and reached under `test` flavor in LIVE mode.

1. `settings.gradle.kts` + root `build.gradle.kts` (AGP 8.11.1, Kotlin 2.1.21, compose plugin, `include(":app", ":sidecar")`, repos) + `gradle/libs.versions.toml`.
2. `app/build.gradle.kts` rewrite (flavors, buildConfig fields per §1.4, deps + flags) + minimal `AndroidManifest.xml` + `res/values/{strings,colors,drawable}` + `gradle.properties`.
3. `com.agnes.bundle_agnes.AgnesApplication` (AppContainer init) + `MainActivity` (Compose setContent, nav host Splash→Auth).
4. `com.sobrr.agnes.data.model.*` (BaseResponse envelope/DTOs) + `com.sobrr.agnes.data.network` (`Api`, `ApiResult`, `ApiResultCall`), Retrofit factories, `AuthApi` service.
5. Keystore token Store + DataStore prefs + `MockUserRepository`/`MockAuthRepository` (bound under `API_PROFILE=MOCK`).
6. `feature.chat`: `SseParser` (partial-chunk assembler), `StreamBlock` sealed class, `MockSseSource` (scripted Flow, no HTTP), `ChatViewModel` + initial Compose renderers (thinking/skill_load/tool_call/text/image/artifact/followups/error/done).
7. `backend-sidecar/` scaffold: Ktor `Application`, `/healthz`, `.env.example`, `model-routing.yaml`, `AgnesGatewayContracts.kt`, `MockSseEmitter` for sidecar-MOCK contract runs.
8. Wire `ChatApi` Retrofit behind `API_PROFILE=LIVE` → sidecar routes verified via contract tests; run `./gradlew :app:assembleDebug` and `./gradlew :sidecar:test`.

Everything above is implementation detail an ACT-writer can follow in order; commit nothing until asked.