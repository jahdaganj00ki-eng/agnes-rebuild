# API Surface (rebuild contract)

All endpoints are REST/JSON behind the per-environment base URLs (see README §2) and use the
uniform envelope `BaseResponse<T>` (or `BaseNoResponse` for void calls). Group them into these
Retrofit interfaces — names mirror the model DTOs in the package map:

## AuthApi
- `POST /auth/login` — e-mail/password (`EmailAndPassword`), phone (`PhoneAndPassword`)
- `POST /auth/google` — Google token exchange (`GoogleToken` → `GoogleLoginResult`)
- `POST /auth/register/email` (`RegisterByEmailRequest`)
- `POST /auth/bind/email` (`BindEmailRequest`), `POST /auth/bind/phone` (`BindPhoneRequest`)
- `GET /auth/me` → `AuthMeUserDto` · `POST /user/username` (`ChangeUserName`)
- `GET /config/login` → `LoginConfig` · `GET /config/feature` → `FeatureConfig`

## ChatApi
- Conversation CRUD: list / detail / history / search / delete
  (`ConversationList`, `ConversationDetail`, `ConversationHistory`, `ConversationSearchRes`)
- `POST /chat/stream` (`ChatStreamRequestBody`) — **streamed** completion (chunked/SSE)
- `POST /chat/resume-stream` (`ChatResumeStreamRequestBody`)
- `POST /chat/hitl/resume` (`ChatHitlResumeRequestBody`) — human-in-the-loop continue
- `POST /chat/regenerate` (`ChatRegenerateRequestBody`)
- `POST /conversation/title` (`ConversationTitleSummary`)
- Preset replies & follow-ups: `PresetRepliesPayload`, `FollowUpQuestions`

## CharacterApi
- `POST /role/avatar` (`CreateRoleAvatarRequest`)
- `POST /generate/candidate-images` · `/generate/personality-brief` · `/generate/opening-line`
- `POST /role/draft` (`CreateRoleDraft`) · character detail/content fetch
- Projects & assets: `ProjectsListResponse`, `DeleteProjectListResponse`, `DeleteAssetsRequest`,
  `AvatarPresignedUrlRequest`

## CommunityApi
- `GET /community/list` (`CommunityListRequest` → `PostCommunityItem[]`)
- `POST /community/post` (`PostCommunityRequest` with `PostCommunityContent` incl. media)
- emoji reacts `EmojiRequest`/`CancelEmojiParam`, comments `CommentRequest`/`CommentListRequest`

## GameApi
- `GET /game/list` · `GET /game/category-tree` → `GameCategoryTreeResponse`
- `GET /game/{id}` → `GameDetailItem`/`GameInfoResponse` · intro (`GameIntroParams`)
- `POST /game/ugc` (`CreateUgcGameRequest`) · template generation (`GenerateTemplateResponse`)
- Groups: `GameGroupsModel`, members, custom info; `POST /game/join-by-share-code`
- `POST /game/help-answer` (`GameHelpAnswerRequest`)

## TaskApi
- Task list (typed by task enums), records (`RecordItem`), points transactions
  (`PointsTransactionRequest/Response` with `PointsPagination`)

## BillingApi
- Plans: `GooglePlan`/`PlanPrice`/`PricingPhase` · `GET /billing/current`
- `POST /billing/google-pay` (`GooglePayRequest`) · `POST /billing/cancel`
- One-time offers: `OneTimePurchaseOfferDetails`, `CreditsPacksBean`, `GoogleFuelPack`
- Economy: `ModelsAccess*`, `ModelsCost*`, `QuotaLog`, `PptUpgradeGateCheckResponse`

## UploadApi / VisionApi
- `POST /upload/presigned-url` → `PresignedUrlResponse` (multipart PUT to storage)
- `AvatarPresignedUrl*`, `delete/confirm` asset endpoints
- OCR: `AgnesImageOcrRequest` · element recognition `ImageElementRecognition*`

## FiltersApi
- Content-filter settings fetch/update (`feature_filters/network/request|response` DTOs),
  cached locally in `AgnesFilterDatabase` (Room).
