# Agnes-Style AI Companion App — Complete Rebuild Specification

> **What this repository is for.** This repo is a *complete technical rebuild blueprint* of the
> Android app **“Agnes”** (`com.sobrr.agnes`, v3.0.61, target SDK 36 / min SDK 26), derived from a
> structural analysis of the released Android App Bundle. It contains **no original source code**
> (the shipped app is proprietary and R8-obfuscated) — instead it documents the architecture,
> feature set, data layer, API surface and tooling in enough detail that an AI coding agent
> (Claude Code, Cursor, …) can rebuild a functionally equivalent app from scratch.
> Hand this repo to the **GitReverse** method (replace `github.com` with `gitreverse.com` in the
> repo URL) and it will turn README + file tree into a “build this app” prompt.

---

## 1. Product Summary

**Agnes is a hybrid AI-companion super-app.** A native Kotlin Android shell hosts an
H5/web application (served from `https://app.agnes-ai.com/`) plus a set of native feature
modules for AI chat, character creation, a community feed, UGC mini-games, a task/reward
system, monetization, media picking and cloud uploads.

Core user experience:

1. **Sign up / login** via Email+Password, Phone, or Google (Android Credential Manager).
2. **Chat with AI characters** — streamed text answers (SSE-style streaming chat), voice/audio
   playback, preset replies, follow-up questions, conversation history/search, multi-conversation
   management, human-in-the-loop review flows and regeneration.
3. **Create your own AI characters** — a wizard that generates avatar candidates, personality
   briefs and opening lines server-side, then publishes a role/character.
4. **Community feed** — posts with text/media, emoji reactions and comments.
5. **UGC games** — create, browse (category trees), join and share text-based group games/role
   plays, including share codes and group chat integration.
6. **Tasks & rewards** — daily/one-time tasks granting credits/points with transaction history.
7. **Monetization** — monthly subscriptions plus one-time *credit/fuel packs* via Google Play
   Billing; quota, cost and model-access gating explains what features consume credits.
8. **Media pipeline** — custom photo picker and multi-part cloud uploads using presigned URLs.

## 2. Runtime Architecture (the most important design decision)

* **Native container + H5.** Large parts of the product UI (e.g. marketing pages, settings,
  profiles, parts of the feed) are **H5 pages** inside WebViews, bridged to native code.
  Performance-critical or hardware-touching features are native Kotlin modules.
* **API base URLs** (environment aware):
  * prod API `https://api.agnes-ai.com`, prod H5 `https://app.agnes-ai.com/`
  * preview API `https://api-preview.agnes-ai.com`, preview H5 `https://preview.agnes-ai.com/`
  * dev/test environments exist for staging (`kiwiar.com` domain family).
* **App entry points:** `AgnesApplication` (Application class: init DI, Firebase, push, Trackers,
  Room, DataStore caches) and a single-activity `MainActivity` that manages WebView + native
  screens and navigation.
* **Push:** Firebase Cloud Messaging (`FcmService`) plus Tencent IM push channels
  (TIM/TUICore `TIMPush*`) for chat messages.

## 3. Tech Stack (recoverable from the APK)

| Layer | Technology |
|---|---|
| Language | Kotlin (Kotlin coroutines, `kotlinx`) |
| UI | Android Views + RecyclerView/ConstraintLayout for list-heavy screens; **Jetpack Compose** incl. `org.jetbrains.compose` resources for the chat “conversation” components |
| Networking | Retrofit 2 + OkHttp 3 + Gson; custom `ApiResult`/`ApiResultCall`/`ApiResultAdapterFactory` wrapper for typed success/error handling |
| Local storage | **Room 3** (`androidx.room3`) — `AgnesFilterDatabase`, `PhotoPickerDatabase`; **DataStore** — caches such as `CommunityCacheDataStore`, `GameListCacheDataStore`, credits cache |
| Chat / IM | **Tencent IM SDK** (`tencent.qcloud.tim`, `tuicore`) + in-house conversation components (`ui/chat/components/conversation/*`) with design-system-generated summary blocks |
| Media | androidx.media3 (ExoPlayer) for audio/video; custom photo-picker module |
| Auth | E-mail + password, phone, Google Sign-In via AndroidX **Credentials** / GMS auth |
| Payments | Google **Play Billing** library (subscriptions + one-time products, incl. V2 proxy activities) |
| AI backend | **Agnes AI gateway** (OpenAI-compatible, `apihub.agnes-ai.com`) — full integration spec in [docs/04-ai-gateway.md](docs/04-ai-gateway.md) |
| Analytics / Attribution | Firebase Analytics (AppMeasurement), **Adjust** SDK, **AppsFlyer** SDK, Firebase Install Referrer |
| App platform | Firebase Core/Sessions, Play Core, profileinstaller, androidx.startup, emoji2, window, credentials, datastore |

## 4. Module / Package Map

The file tree under `app/src/main/java/` mirrors the real package layout (placeholder files —
by design). Modules:

```
com.agnes.bundle_agnes        → app bundle glue: AgnesApplication, MainActivity, BuildConfig
  └─ ui/chat/components/conversation/*   → ~50 classes: native AI chat screen (Compose),
  │                                         conversation list, streaming message rendering,
  │                                         design-system blocks, normal ViewModel, enums
  └─ ui/game/model, db                    → game UI models, Room DAO wiring
com.agnes.feature_billing/net/model       → ~23 DTOs: subscriptions, plans, prices, pricing
                                            phases, GooglePay requests, cancel-subscription,
                                            current subscription, one-time purchase offers
com.agnes.feature_community/model|repository → ~27 DTOs + repository: posts, comments,
                                            emoji reacts, list/detail requests
com.agnes.feature_game/model|repository   → ~99 DTOs + repository: game catalog, category
                                            trees/tags, game detail/intro, UGC game creation,
                                            group chats/infos/members, share-code join,
                                            help answers, name maps
com.agnes.feature_task/{enums,net/model}  → ~39 DTOs + enums: tasks, records, points
                                            transactions with pagination
com.agnes.feature_push/{net/model,service}→ FcmService + push payload models
com.agnes.feature_photo_picker/db         → Room entities/DAOs for the photo picker
com.agnes.upload/internal                 → FileUploadApi, multipart uploads, presigned-URL
                                            requests/responses, token decryption
com.sobrr.agnes.data/model/*              → ~100 shared models: base responses
  │                                         (BaseResponse/BaseNoResponse, Pagination),
  │                                         media (MediaData, MediaType, MediaSource),
  │                                         cloudcustomdata, gson adapters, pushData,
  │                                         pixa/* (~24 classes: dynamic “Pixa” layout system —
  │                                         pages, modes, features, background items,
  │                                         staggered grid configs)
com.sobrr.agnes.data/network              → Api client, intercepted requests, file upload,
                                            ApiResult machinery
com.sobrr.agnes.feature_auth/model        → ~46 DTOs: AuthEmail/AuthGoogle/AuthPhone,
                                            register-by-email, bind email/phone, login config,
                                            AuthScene/Auth-Me user DTO, Google tokens
com.sobrr.agnes.feature_filters/*         → content filter settings: FilterApi, request &
                                            response DTOs, Room storage (AgnesFilterDatabase)
```

## 5. Feature Specifications (what a rebuild must implement)

### 5.1 Authentication (`feature_auth`)
* Scenes driven by `AuthScene`; providers: e-mail+password (`EmailAndPassword`,
  `RegisterByEmailRequest`), phone+password, Google One-Tap via Credential Manager
  (`GoogleToken`, `GoogleLoginResult`).
* Post-login: `AuthMeUserDto` profile fetch; ability to **bind** additional identifiers
  (`BindEmailRequest`, `BindPhoneRequest`, `ChangeUserName`).
* `LoginConfig`/`FeatureConfig`/`GlobalErrorConfig` drive remote-configured behavior.

### 5.2 AI Chat (`ui/chat/components/conversation`, ~50 components)
* Conversation CRUD: list (`ConversationList`, `AgnesMultiConversationList`), search
  (`ConversationSearchRes`), detail/history (`ConversationDetail`, `ConversationHistory`),
  title summarization (`ConversationTitleSummary`), status tracking.
* Streaming chat protocol: `ChatStreamRequestBody`, `ChatResumeStreamRequestBody`,
  `ChatHitlResumeRequestBody` (human-in-the-loop resume), `ChatRegenerateRequestBody`,
  `ChatNewsRequestBody`, `PresetReplies`, `FollowUpQuestions`.
* UI is **Jetpack Compose**; a *design-system summary* resolver maps streamed assistant blocks
  to generated UI blocks (“design system generated block”). Rebuild as: message list with typed
  blocks (text / media / cards / actions) rendered from streamed JSON chunks.
* `ChatNormalViewModel` orchestrates intents incl. subclassed intent handling; enums
  (`EnumC1767n`, `EnumC1773q`, …) encode message/turn states.

### 5.3 Character / Role Creation
* Server-assisted creation: `CreateRoleAvatarRequest/Response`,
  `GenerateCandidateImagesRequest/Response`, `GeneratePersonalityBriefRequest/Response`,
  `GenerateOpeningLineRequest/Response`, `CreateRoleDraft`, `CharacterItem/CharacterDetail/
  CharacterContent`.
* Flow: pick/generate avatar candidate → generate personality brief → generate opening line →
  publish character; management via project/asset objects (`ProjectItem`, `DeleteProject*`,
  `AssetItem`, `DeleteAssetsRequest`).

### 5.4 Community (`feature_community`)
* Feed of `PostCommunityItem` (content with media), creating posts (`PostCommunityRequest`),
  emoji reactions (`EmojiRequest`/`CancelEmojiParam`), comments (`CommentRequest`,
  `CommentListRequest`, `CancelCommentParam`), delete (`DeletePostRequest`).
* Client cache: `CommunityCacheDataStore`; `CommunityListRequest` drives pagination.

### 5.5 UGC Games (`feature_game`, biggest module)
* Catalog: `GameListResponse`, free/recommendation dismissal via `GameListCacheDataStore`.
* Taxonomy: category tree (`GameCategoryTree*` models with categories/tags/items).
* Detail & intro: `GameDetailItem`, `GameInfo*`, `GameIntroParams`, `ChapterInfo/ChapterItem`.
* Creation: `CreateUgcGameRequest/Response` (+ `GenerateTaskData`, templates).
* Social: game **group chats** (`GameGroupChatModel`, `GameGroupsModel`, `GameGroupDetailData`,
  `GameMembersInfo`), join by share code (`JoinGameByShareCodeRequest`), help Q&A
  (`GameHelpAnswerRequest`), name maps, custom group info.
* Backed by `game/model` + Room (`db`) wiring.

### 5.6 Tasks & Points (`feature_task`)
* Task list with typed enums, completion records (`RecordItem`), points ledger
  (`PointsTransactionRequest/Response`, `PointsPagination`), migrate-waiting state
  (`MigrationWaitingResponse`).

### 5.7 Billing (`feature_billing`)
* Google Play Billing integration: subscription plans (`GooglePlan`, `PlanPrice`,
  `PricingPhase`, `CurrentSubscription`), one-time *fuel/credits packs* (`GoogleFuelPack`,
  `CreditsPackItem`, `CreditsPacksBean`, `OneTimePurchaseOfferDetails`),
  order submission (`GooglePayRequest`), cancel (`CancelSubscriptionRequest`).
* Economy introspection APIs: `ModelsAccess*` (which AI models/features are unlocked),
  `ModelsCost*` (how many credits an action costs), `QuotaLog`.

### 5.8 Media, Upload & Vision
* Photo picker: native picker UI persisted in `PhotoPickerDatabase`; media model
  (`MediaData`, `MediaType`, `MediaSource`, `ImageSize`).
* Uploads: `FileUploadApi`, `MultipartUploadApi`, presigned URL flow
  (`PresignedUrlRequest/Response`, `AvatarPresignedUrlRequest/Response`), token decryption,
  `delete`/`confirm` asset slots.
* On-device/server vision: `AgnesImageOcrRequest` (OCR) and
  `ImageElementRecognitionRequest/Response` (UI element understanding), batches/element items.

### 5.9 Dynamic UI system (“Pixa”)
* ~24 models describing **server-driven layouts**: pages (`PixaLayoutPage`), modes
  (`PixaModeData/Item`), features (`PixaFeatures*`), backgrounds (`BackgroundItem`),
  staggered grids (`BaseStaggerGridModel`). Rebuild as: JSON-driven screen/layout renderer.

### 5.10 Push & Platform
* `FcmService` (Firebase Messaging) + Tencent `TIMPush` suite; payload models under `pushData`.
* Deep integrations: Play Core dialogs, Adjust/AppsFlyer attribution, Firebase Analytics,
  startup/profileinstaller/emoji2/window androidx libs.

## 6. Data & Networking Conventions

* All REST calls return a uniform envelope: `BaseResponse<T>` / `BaseNoResponse` (code/message/
  data), surfaced through the custom `ApiResult` call adapter — rebuild this pattern.
* Pagination via `Pagination`/`PaginationInfo`; uploads via presigned URLs; auth tokens attached
  by OkHttp interceptors; Gson for (de)serialization with custom adapters (`data/model/gson`).
* Local-first caching for feed/games/credits (DataStore) and structured offline data (Room 3).

## 7. Android Manifest Rebuild Notes

* `package com.sobrr.agnes` (applicationId), `AgnesApplication`, single `MainActivity`,
  `FcmService`, `FileProvider`.
* Permissions to declare: `INTERNET`, `CAMERA`, `RECORD_AUDIO`, `READ_MEDIA_IMAGES`,
  `READ_MEDIA_VIDEO`, (legacy storage for API 26), `POST_NOTIFICATIONS`, `VIBRATE`,
  `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `AD_ID`, Play `BILLING`, FCM `RECEIVE`,
  install-referrer binding, Access AdServices.
* Manifest also hosts: Play Billing proxy activities, Firebase messaging/analytics receivers &
  components, Tencent push activities/provider, Adjust content provider, Room3
  multi-instance invalidation service, androidx startup/profileinstaller, compose resources
  provider, Credential-Manager metadata holder.
* Deliver as **Android App Bundle** with split configs: `arm64_v8a`, `en`, `hdpi`
  (matches original packaging: base apk + config splits, 64-bit only).

## 7.5 AI Provider (decided for this rebuild)

All generative features (streaming chat, character avatars, personality/opening-line
generation, vision) are served through the **Agnes AI gateway** (`https://apihub.agnes-ai.com/v1`,
OpenAI-compatible) with models `agnes-2.5-flash`, `agnes-1.5-flash`, `agnes-image-2.1-flash`.
The Android client never calls the gateway directly: your own backend proxies it, holds the
API key and enforces the subscription gating described in §5.7 — exactly like the original
app's client→backend split. Full mapping, wire formats and quotas: **docs/04-ai-gateway.md**.

## 8. Suggested Rebuild Order (for the coding agent)

1. Scaffold: app module, `applicationId com.sobrr.agnes`, min 26 / target 34+, Kotlin, Gradle
   version catalogs; wire Firebase BOM, Billing, Tencent IM, Retrofit/OkHttp/Gson, Room 3,
   DataStore, media3, Credentials, Adjust, AppsFlyer.
2. `data` layer: `ApiResult` envelope, Retrofit service interfaces per feature, interceptor
   chain, Room DBs, DataStore caches — mirroring package map in §4.
3. Auth flow (5.1) → gated home; navigation shell with WebView/H5 bridge.
4. Chat (5.2): conversation list → streaming chat screen (Compose) → character creation (5.3).
5. Community (5.4) → Games (5.5) → Tasks (5.6).
6. Billing (5.7) + quota/model gating; Uploads/Vision (5.8); Pixa renderer (5.9).
7. Push, analytics, deep links, release packaging (7).

## 9. Legal / Scope Notice

This repository is a **reverse-engineered specification for interoperability and learning**.
It intentionally contains **no code, assets, strings, or other copyrighted material** from the
original application. “Agnes”, backend domains, trademarks and all rights remain with their
respective owner. Rebuilt software must not connect to the original production backends or use
the original brand without permission.
