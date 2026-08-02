# Agnes-Style AI Companion App — Full Rebuild Specification (v3.0.61)

> **Purpose.** This repository is a complete rebuild blueprint of the Android app **“Agnes”**
> (`com.sobrr.agnes`, v3.0.61, min SDK 26 / target SDK 36), derived from a structural analysis of
> the released App Bundle. It contains **no original source code** (the app is proprietary and
> R8-obfuscated). Everything an AI coding agent needs is here: exact API bindings, the agent/skill
> runtime, data models, screens, packaging, and the AI-provider integration.
> Feed this repo to **GitReverse** (`gitreverse.com` instead of `github.com` in the URL) and use
> the generated prompt with Claude Code / Cursor to rebuild the app end-to-end. Docs: `docs/`.

---

## 1. Product Summary — what you are building

A **hybrid AI-companion super-app**: a native Kotlin Android shell around an H5 front-end
(`app.agnes-ai.com`) with deep native modules. Feature surface:

1. **Auth & profile** — email + password, phone + code, Google (Credential Manager); bind
   email/phone later; avatar upload; account deletion; timezone; push-token lifecycle.
2. **Agent chat** — streamed conversations against a tool-using agent: thinking blocks,
   skill-loading blocks, tool-call cards (files, search, image gen, code exec), follow-up
   questions, preset replies, regenerate / resume / cancel / human-in-the-loop resume.
3. **Artifacts** — the agent produces downloadable artifacts per conversation: files, slide
   decks (PPT/PPTX), even **publishable websites**; per-artifact share & download endpoints.
4. **Image studio** — text→image and image→image edit flows (reference image + prompt-craft),
   OCR, watermark removal, user visuals/materials libraries.
5. **Characters** — guided creation (avatar candidates → personality brief → opening line →
   publish), template gallery with like/collect/dislike and drafts.
6. **Community feed** — posts with media, emoji reacts, comments.
7. **UGC games** — category-tree catalog, detail/intro, group chats via share codes, help Q&A.
8. **News** — personalized news feed (categories, search, read-stats).
9. **Tasks & rewards + referral** — task system with points ledger; invitation codes with
   stats/log and redeem flow.
10. **Monetization** — subscriptions + credit packs via Google Play Billing (v2 verify),
    credits balance, plan gating, upgrade gates (e.g. PPT).
11. **Platform** — FCM push + Tencent IM, photo picker, multipart uploads (presigned URLs),
    room caches, DataStore caches, Adjust/AppsFlyer attribution.

## 2. Runtime architecture

* **Native shell + H5.** `MainActivity` (single activity) hosts WebViews for product pages and
  native screens for chat, camera, picker, store. JS bridge exposes: auth token, navigation,
  photo picker, upload, billing trigger, share, push state.
* **Backends** — app backend per environment (Retrofit/Gson, envelope
  `BaseResponse<T>` {code,message,data} / `BaseNoResponse`, pagination via `PaginationInfo`):
  prod `https://api.agnes-ai.com` · preview `https://api-preview.agnes-ai.com` · dev/test on
  `kiwiar.com` domains. **AI generation is proxied by the backend** to the Agnes AI gateway
  (`apihub.agnes-ai.com`, see §8) — the client never calls it directly.
* **Streaming** — chat answers arrive as a chunked/SSE stream of typed blocks (see §7) and are
  rendered natively as Compose “design-system” blocks.

## 3. Module / package map (mirrored by `app/src/main/java/…`)

```
com.agnes.bundle_agnes        AgnesApplication, MainActivity, BuildConfig
 ├─ ui/chat/components/conversation/*   ~50 Compose components: message list, stream blocks,
 │                                       SkillLoadedDisplay, thinking accordion, enums
 └─ ui/game/model, db                  game UI models + Room wiring
com.agnes.feature_billing/net/model    ~23 DTOs (plans, prices, offers, cancel, verify…)
com.agnes.feature_community/model|repository   ~27 DTOs + repo (posts, emoji, comments)
com.agnes.feature_game/model|repository        ~99 DTOs + repo (catalog, groups, share codes)
com.agnes.feature_task/{enums,net/model}       ~39 DTOs + ToolCallEnum (agent tool types!)
com.agnes.feature_push/{net/model,service}     FcmService + payload models
com.agnes.feature_photo_picker/db              picker persistence
com.agnes.upload/internal                      FileUploadApi, multipart, presigned flow
com.sobrr.agnes.data/model/*            base envelope, media, gson adapters, pushData,
                                        cloudcustomdata, pixa/* (server-driven layout system)
com.sobrr.agnes.data/network            Retrofit wiring, ApiResult call-adapter, interceptors
com.sobrr.agnes.feature_auth/model      ~46 DTOs (email/phone/google, bind flows, tokens)
com.sobrr.agnes.feature_filters/*       content-filter settings (FilterApi + Room)
```

## 4. Tech stack

| Layer | Choice |
|---|---|
| Language / async | Kotlin + coroutines |
| UI | Views + RecyclerView/ConstraintLayout; **Jetpack Compose** for chat blocks (`org.jetbrains.compose` resources) |
| Network | Retrofit2 + OkHttp3 + Gson, `ApiResult`/`ApiResultCall`/`ApiResultAdapterFactory` |
| Local | Room3 (`AgnesFilterDatabase`, `PhotoPickerDatabase`), DataStore (community/game/credits caches) |
| IM / chat infra | Tencent IM SDK (`tencent.qcloud.tim`, TUICore, TIMPush) |
| Media | androidx.media3 (voice playback), custom photo picker |
| Auth | androidx Credentials + Google Sign-In (GMS auth) |
| Billing | Play Billing (subs + one-time), `/v2/purchase` + `/subscription/verify-payment` |
| Analytics | Firebase Analytics, Adjust, AppsFlyer, install referrer |
| Push | Firebase Messaging (`FcmService`) + TIMPush |
| AI backend | **Agnes AI gateway** OpenAI-compatible — §8 |

## 5. Complete API binding map (real methods + paths)

### 5.1 Auth & account
```
POST /api/auth/token_by_email            email one-shot token login (AuthEmail)
GET  /api/auth/me                        current user (AuthMeUserDto)
DELETE /api/auth/me                      unlink identity
GET  /api/auth/clear_firebase_token      logout cleanup
POST /api/auth/update_user_avatar        (ChangeAvatar via processed file)
POST /api/auth/update_user_name          (ChangeUserName)
POST /api/v1/user/login                  email/phone+password (EmailAndPassword/PhoneAndPassword)
POST /api/v1/user/register               (RegisterByEmailRequest)
POST /api/v1/user/refresh-token          token refresh
POST /api/v1/user/code/send              send verification code
POST /api/v1/user/code/verify            verify code
POST /api/v1/user/bind_email             BindEmailRequest
POST /api/v1/user/bind_phone             BindPhoneRequest
POST /api/v1/user/reset_password
POST /api/v1/user/timezone               tz sync
PUT  /api/v1/user/profile                profile update
GET  /api/v2/user/profile                profile v2 (OwnerUser/MineInfoResponse)
DELETE /api/v1/user/account              account deletion
POST /api/v1/fcm/token                   FCM registration
GET  /api/v1/user/migration/waiting      account migration banner (MigrationWaitingResponse)
GET  /api/v1/version/check               app update check
POST /api/v1/security/parameter          signed anti-abuse parameter bundle
```

### 5.2 Agent chat (`/api/v1/agnes/*`)
```
POST /api/v1/agnes/chat/stream              SSE stream start (ChatStreamRequestBody;
                                            fields: conversation_id, message, tool_mode,
                                            scene, agent_type, attachments, model_code)
POST /api/v1/agnes/chat/stream/cancel       cancel running turn
POST /api/v1/agnes/chat/stream/resume       resume interrupted stream
POST /api/v1/agnes/chat/stream/regenerate   regenerate last answer (ChatRegenerateRequestBody)
POST /api/v1/agnes/chat/stream/hitl-resume  human-in-the-loop resume (ChatHitlResumeRequestBody)
GET  /api/v1/agnes/conversations            conversation list (AgnesMultiConversationList)
POST /api/v1/agnes/conversation             create conversation
GET  /api/v1/agnes/conversation             detail
PATCH /api/v1/agnes/conversation            update (rename/pin)
DELETE /api/v1/agnes/conversation           delete
GET  /api/v1/agnes/conversation/history     message history (paginated)
GET  /api/v1/agnes/conversation/search      search
GET  /api/v1/agnes/conversation/running     running turns (reconnect UX)
POST /api/v1/agnes/conversation/title-summary auto-title (ConversationTitleSummary)
GET  /api/v1/agnes/agnes-chats              agent/character chat index
GET  /api/v1/agnes/follow-up-questions      suggested follow-ups (FollowUpQuestions)
GET  /api/v1/agnes/mode_support_models      per-scene model catalog (ModeSupportModelsResponse)
GET  /api/v1/agnes/daily-hot-topics/latest  hot topics
GET  /api/v1/agnes/recommend-topics         topic prompts
POST /api/v1/agnes/recommend-topics/refresh refresh topics
GET+POST /api/v1/agnes/ai_voice/tts-toggle  voice playback preference
POST /api/v1/agnes/image_ocr                OCR (AgnesImageOcrRequest)
POST /api/v1/agnes/ppt/upgrade-gate/check   PPT feature gate (PptUpgradeGateCheckResponse)
POST /api/v1/agnes/website/publish          publish generated website artifact
GET  /api/v1/agnes/user/materials          user material bin
GET  /api/v1/agnes/user/visuals            user generated visuals
```

### 5.3 Artifacts (agent outputs per conversation)
```
GET /api/v1/agnes/conversation/by-id/{conversationId}/artifacts/list
GET /api/v1/agnes/conversation/by-id/{conversationId}/artifacts/{eventId}/download
GET /api/v1/agnes/conversation/artifact-share           share artifact (GetArtifactShareReply)
GET /api/v1/file/conversation/sandbox                   agent sandbox files (ImChatReloadDiskPayload)
GET /api/v1/file/conversation/uploads                   user uploads of conversation
```

### 5.4 Files & uploads
```
POST /api/file/presigned-url                  presigned URL v1
POST /api/v1/file/presigned-url               presigned URL v1.x (PresignedUrlRequest/Response)
POST /api/v1/file/multipart/init              start multipart
POST /api/v1/file/multipart/complete          finish multipart
POST /api/v1/file/multipart/abort             abort multipart
POST /api/v1/user/avatar/presigned-url        avatar upload
POST /api/file/process/chat                   post-process chat attachments
POST /api/file/process/avatar                 post-process avatar
GET  /api/v1/aigc/user-assets                 AIGC asset bin
POST /api/v1/visuals/remove_watermark         watermark removal
```

### 5.5 AIGC templates & characters
```
GET  /api/v1/template/categories
GET  /api/v1/template/list_by_category
GET  /api/v1/template/list_by_ids
GET  /api/v1/template/drafts            · POST /api/v1/template/drafts · DELETE .../drafts
POST /api/v1/template/generate          template-based generation
POST /api/v1/template/upload            publish template
POST /api/v1/template/like
POST /api/aigc/template/drafts/create   · GET /api/aigc/template/drafts/list
POST /api/aigc/template/search
POST /api/aigc/template/collect/{template_id}     · /uncollect/{template_id}
POST /api/aigc/template/dislike/{template_id}
GET  /api/group_stencil/stencil_detail/{template_id}
```

### 5.6 Games & IM
```
GET  /api/v1/game/list                  catalog (GameListResponse)
GET  /api/v1/game/list-by-show-type     free/recommended splits
GET  /api/v1/game/catalog/category-tree taxonomy (GameCategoryTreeResponse)
GET  /api/v1/game/games/{game_id}       detail (GameDetailItem/GameInfoResponse)
GET  /api/v1/game/info/search
GET  /api/v1/game/game-name-map         localized names (GameNameMapInfo)
GET  /api/v1/game/user-assets
GET  /api/v1/game/profile/{userId}      · POST /api/v1/game/profile/update
POST /api/v1/game/groups                create game group
POST /api/v1/game/groups/join           · /quit
GET  /api/v1/game/groups/active-by-game
GET  /api/v1/game/groups/info
POST /api/v1/game/help-answer           (GameHelpAnswerRequest)
GET  /api/v1/im/user-sig                Tencent IM UserSig (login to IM SDK)
GET  /api/v1/im/group-share-codes/{shareCode} resolve share code (JoinGameByShareCodeRequest)
DELETE /api/v1/im/groups/{groupId}
```

### 5.7 News
```
GET /api/v1/news/list_with_custom       personalized feed
GET /api/v1/news/detail                 · GET /api/v1/news/search
GET /api/v1/news/user-categories        · PUT /api/v1/news/user-categories
POST /api/v1/news/record-read           · GET /api/v1/news/read-stats
GET /api/v1/news/recent-reads           · POST /api/v1/news/clear-reads
POST /api/v1/news/preference            interest prefs
```

### 5.8 Monetization
```
GET  /api/v2/subscription/plans              plans (GooglePlan/PlanPrice/PricingPhase)
GET  /api/v2/subscription/credits-balance    wallet
GET  /api/v1/subscription/credits-packs      one-time packs (CreditsPacksBean/GoogleFuelPack)
POST /api/v1/subscription/credits-transactions ledger (PointsTransactionRequest/Response)
POST /api/v1/subscription/verify-payment     Play receipt verification
POST /v2/purchase                            v2 purchase verify
POST /api/v1/subscription/cancel             cancel (CancelSubscriptionRequest)
```

### 5.9 Invitation / referral
```
GET  /api/v1/invitation/config          share assets/config
POST /api/v1/invitation/code            my code
POST /api/v1/invitation/code/{code}/redeem
GET  /api/v1/invitation/log             · GET /api/v1/invitation/stat
```

### 5.10 Community (DTO-bound; paths are constructed via constants)
Posts: list/create/delete (`PostCommunityRequest`, content incl. media), emoji react
(`EmojiRequest`/`CancelEmojiParam`), comments (`CommentRequest`/`CommentListRequest`/cancel),
feed pagination via `CommunityListRequest` + `CommunityCacheDataStore`.

## 6. Screens & navigation

Splash(remote config, push permission) → Auth scene (email/phone/Google; bind flows) →
Home shell (Chat · Community · Games · News · Me). Chat: conversation list (search, running
badge) → streaming conversation (blocks §7; attachments; TTS toggle; regenerate/cancel/resume).
Artifact viewers: file preview, slide pages + pptx download, website preview + publish.
Store: plans/pricing phases, credit packs, quota/model gating. Profile: filters, settings,
referral (invitation) center, account deletion. H5 pages: help/legal via WebView.

## 7. Agent runtime & skills (rebuild spec — full research in `SKILLS-RECHERCHE.md` / `docs/05`)

* **Stream block protocol**: SSE chunks typed as blocks: `thinking` (accordion
  `thinkingExpandMap`), `skill_load` (renders `SkillLoadedDisplay`), `tool_call` cards,
  `text`, `image`, `artifact`, `followups`, `error`. Design-system resolver maps blocks to
  Compose components.
* **Tool-call enum** (server → client cards): `LoadSkill`, `GenerateImage`, `WebSearch`,
  `ImageSearch`, `ReadFile`, `WriteFile`, `EditFile`, `ListFiles`, `Execute`, `WriteReport`,
  `QueryWeather`, `ProfileData`, `Other` — OpenAI-style `tool_call_id` correlation.
* **Skill store** (server-side, SKILL.md dirs + sub-references, e.g. `image-generation/` +
  `reference-image` doc, `image-prompt-craft`, `slides/ppt`). Edit-Image flow observed:
  client sends `tool_mode="text_edit_image"` + `image_gen_prompt`; agent loads
  `image-generation` → `image-generation/reference-image` → `image-prompt-craft` →
  `GenerateImage` (reference = user upload URL).
* Client only orchestrates; all LLM/image calls happen server-side via §8.

## 8. AI provider: Agnes AI gateway (backend-side)

* OpenAI-compatible: `POST {AGNES_BASE_URL}/chat/completions`, default
  `AGNES_BASE_URL=https://apihub.agnes-ai.com/v1`; alternate `…agnes-ai.cn/v1` (network
  failures only), China `api.agnes-ai.cn/v1`. Auth `Bearer $AGNES_API_KEY` **on the backend
  only** (register key at `platform.agnes-ai.com`, docs `agnes-ai.com/doc/overview`).
* Models: `agnes-2.5-flash` (chat/stream/tools/vision, ~512K ctx), `agnes-1.5-flash`
  (cheap/titles), `agnes-image-2.1-flash` (`/v1/images/generations`; `2.0` alt),
  `agnes-video-v2.0` (`/v1/videos`; poll `GET /agnesapi?video_id=<id>`).
* Retry w/ backoff only on `408,429,500–504,520,522,524`. Rules in `docs/04-ai-gateway.md`;
  never route-hop on 4xx; don't double-append `/v1`.
* **Limits/quotas** (access types free/enterprise/token-plan; resolution-dependent image RPM;
  video 500 s/day plan quotas; full tables + error-code→UX matrix): `docs/04-ai-gateway.md` §6.

## 9. Local persistence & caches

Room3: `AgnesFilterDatabase` (content-filter prefs), `PhotoPickerDatabase`, game DB wiring.
DataStore: community feed cache, game list cache (free/dismissed ids), credits cache,
`agnes_prefs`, `thinkingExpandMap` UI state. File uploads via presigned URLs; downloads
behind `/file/download/…`.

## 10. Manifest & packaging

`applicationId com.sobrr.agnes`, `AgnesApplication`, single `MainActivity`, `FcmService`,
`FileProvider`; Billing proxies, Firebase receivers/components, Tencent TIMPush activities/
provider, Adjust provider, Room3 invalidation service, startup/profileinstaller, Credential
metadata holder. Permissions: INTERNET, CAMERA, RECORD_AUDIO, READ_MEDIA_IMAGES/VIDEO,
legacy storage ≤32, POST_NOTIFICATIONS, VIBRATE, WAKE_LOCK, AD_ID, BILLING, C2DM RECEIVE,
install-referrer, AD_SERVICES. Ship as **AAB** (splits: arm64-v8a · en · hdpi), R8 full mode,
version `3.0.61 (3000061)`.

## 11. Rebuild order (for the coding agent)

1. Scaffold (Gradle catalogs, flavors dev/test/preview/prod w/ `BASE_URL`+`H5_URL`).
2. `data`: envelope+`ApiResult` adapter, Retrofit services per §5, interceptors (auth token,
   `security/parameter` signing), Room3 DBs, DataStore caches.
3. Auth flow + profile → shell navigation + WebView bridge.
4. Chat core: list → stream client (SSE parser → block renderer → Compose components) →
   regenerate/cancel/resume/hitl.
5. Artifacts (files/PPT/website viewers + share/download), attachments upload pipeline.
6. Image studio, characters/templates, community, games(+IM), news, tasks/points, referral.
7. Billing v1+v2, quota gating (`mode_support_models`, `ppt/upgrade-gate`).
8. Push (FCM+TIM), analytics/attribution, TTS voice, filters, migration/version UX.
9. Backend sidecar: gateway proxy (§8), skill store + agent loop (§7).
10. Release hardening per §10.

## 12. Acceptance checklist (what “done” means)

[ ] Login all 3 providers incl. bind flows · [ ] streaming chat renders thinking/skill/tool
blocks and survives process death (resume) · [ ] artifacts list + download + website publish ·
[ ] edit-image with reference upload via skills chain · [ ] gallery templates like/collect ·
[ ] community CRUD + emoji/comments · [ ] game join via share code + IM group chat ·
[ ] news prefs feed · [ ] points ledger + referral stats · [ ] purchase verify v1+v2, cancel,
credits balance, upgrade gates · [ ] push delivery end-to-end · [ ] AAB splits build.

## 13. Legal / scope

Reverse-engineered specification for interoperability & learning. **No original code, assets or
strings** reproduced here. All backend paths/identifiers are factual interface data. “Agnes”,
domains, trademarks and servers belong to their owner — do not connect a rebuild to the
original production backends or brand without permission.
