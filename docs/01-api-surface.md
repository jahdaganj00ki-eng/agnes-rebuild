# API Surface — Real Binding Map (extracted interface data)

All endpoints sit behind the per-environment base URLs (README §2) and return the envelope
`BaseResponse<T>` / `BaseNoResponse` (code/message/data). Method+path pairs below are the real
bindings; DTO names refer to the package map (README §3). **The authoritative, complete map
lives in README §5** — this file adds payload wiring notes.

## Retrofit interfaces to generate (one per group)

| Interface | Base prefix | Endpoints | Key DTOs |
|---|---|---|---|
| `AuthApi` | `/api/auth /api/v1/user` | token_by_email, me, clear_firebase_token, login, register, refresh-token, code/send, code/verify, bind_email, bind_phone, reset_password, timezone, profile (GET v2 / PUT v1), account DELETE | AuthEmail/AuthPhone/AuthGoogle, RegisterByEmailRequest, BindEmail/PhoneRequest, AuthMeUserDto, OwnerProfileResponse |
| `ChatApi` | `/api/v1/agnes` | chat/stream (+cancel/resume/regenerate/hitl-resume), conversations CRUD, history, search, running, title-summary, agnes-chats, follow-up-questions, mode_support_models, daily-hot-topics, recommend-topics(+refresh), ai_voice/tts-toggle, image_ocr, ppt/upgrade-gate/check, website/publish, user/materials, user/visuals | ChatStreamRequestBody (conversation_id, message, tool_mode, scene, agent_type, attachments, model_code), ConversationList/Detail/History/SearchRes, PresetReplies, FollowUpQuestions, ModeSupportModelsResponse, AgnesImageOcrRequest, PptUpgradeGateCheckResponse |
| `ArtifactApi` | `/api/v1/agnes/conversation /api/v1/file` | artifacts list/download/share, conversation/sandbox, conversation/uploads | GetArtifactShareReply, ImChatReloadDiskPayload |
| `FileApi` | `/api/file /api/v1/file /api/v1/user` | presigned-url (file + avatar), multipart init/complete/abort, process/chat, process/avatar, aigc/user-assets, visuals/remove_watermark | PresignedUrlRequest/Response, AvatarPresignedUrl*, AssetItem, DeleteAssetsRequest |
| `TemplateApi` | `/api/v1/template /api/aigc/template /api/group_stencil` | categories, list_by_category, list_by_ids, drafts CRUD, generate, upload, like, collect/uncollect/{id}, dislike/{id}, search, drafts/create+list, stencil_detail/{id} | GenerateTemplateResponse, CreateRoleDraft, CharacterItem/Detail, Pagination |
| `GameApi` | `/api/v1/game /api/v1/im` | list, list-by-show-type, catalog/category-tree, games/{id}, info/search, game-name-map, user-assets, profile/{userId}+update, groups create/join/quit/info/active-by-game, help-answer, im/user-sig, im/group-share-codes/{shareCode}, im/groups/{id} DELETE | GameListResponse, GameCategoryTreeResponse, GameDetailItem, GameGroupsModel, GameHelpAnswerRequest, JoinGameByShareCodeRequest |
| `NewsApi` | `/api/v1/news` | list_with_custom, detail, search, user-categories GET/PUT, record-read, read-stats, recent-reads, clear-reads, preference | NewsItem DTOs (news module models) |
| `BillingApi` | `/api/v*/subscription /v2` | v2/plans, v2/credits-balance, credits-packs, credits-transactions, verify-payment, cancel, /v2/purchase | GooglePlan, PlanPrice, PricingPhase, CurrentSubscription, CreditsPacksBean, GoogleFuelPack, OneTimePurchaseOfferDetails, GooglePayRequest, (Cancel)Subscription*, PointsTransaction* |
| `InvitationApi` | `/api/v1/invitation` | config, code, code/{code}/redeem, log, stat | Invitation DTOs |
| `CommunityApi` | DTO-bound (paths via build config constants) | feed list/create/delete, emoji react/cancel, comments list/create/cancel | PostCommunityRequest/Item/Content, EmojiRequest, CommentRequest, CommunityListRequest |
| `SysApi` | `/api/v1` | version/check, security/parameter, fcm/token, user/migration/waiting | FeatureConfig, GlobalErrorConfig, MigrationWaitingResponse |

## Streaming contract (`POST /api/v1/agnes/chat/stream`)

SSE/event-stream chunks, each a typed block (see README §7): `thinking`, `skill_load`,
`tool_call` (with `tool_call_id`; type one of ToolCallEnum: LoadSkill, GenerateImage, WebSearch,
ImageSearch, ReadFile, WriteFile, EditFile, ListFiles, Execute, WriteReport, QueryWeather,
ProfileData, Other), `text`, `image`, `artifact`, `followups`, `error`, terminal `done`.
Client must implement cancel/resume/hitl-resume with the same conversation + last event id.

## Notable request specifics

- `tool_mode` observed hard value: `text_edit_image` (edit-image scene); other scenes are remote-configured (`scene`, `agent_type`).
- `model_code` is app-facing only. Resolve it backend-side via `backend-sidecar/model-routing.yaml` before calling the Agnes gateway.
- Uploads: always two-step (presigned-url → PUT to storage → process/complete call). For vision/image-edit, pass the resulting public or signed image URL to the backend-sidecar.
- Image generation/edit requests should use `size` tiers (`1K`/`2K`/`3K`/`4K`), `ratio`, optional `image: string[]`, and nested `extra_body.response_format`.
- Video task polling must use `video_id` with `/agnesapi?video_id=...`; do not poll current flows with legacy `task_id`.
- `security/parameter`: client computes a signed parameter bundle (anti-abuse) attached to sensitive calls (login, purchase verify).
- Pagination: `Pagination`/`PaginationInfo` (cursor style) on list endpoints.
