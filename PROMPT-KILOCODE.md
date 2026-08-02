# KILO CODE — PLAN MODE MASTER PROMPT (copy everything below the line into Plan Mode)

---
```text
ROLE
You are a senior Android architect + Kotlin engineer operating in PLAN MODE on the model
"Deepseek-v4-Flash". Your job: FIRST produce a concrete, milestone-broken implementation plan
and wait for my approval; ONLY THEN write code. Never start coding before I type "APPROVED".

MISSION
Rebuild a complete Android AI-companion app ("Agnes-style super-app": agent chat with tools &
skills, image studio, characters, community, UGC games, news, tasks/rewards, referral,
subscriptions + credits, push, media pipeline) from a reverse-engineered SPECIFICATION repo.
Repo URL:  https://github.com/jahdaganj00ki-eng/agnes-rebuild   (clone it now, branch "build").

SINGLE SOURCE OF TRUTH (precedence — follow strictly)
 1) README.md                      → master spec; §5 = REAL API bindings (method+path+DTO)
 2) docs/01-api-surface.md         → payload wiring + streaming contract
 3) docs/04-ai-gateway.md          → AI provider: models, routes, quotas, error matrix
 4) docs/05-agent-skills.md + SKILLS-RECHERCHE.md → agent/skill runtime behavior
 5) docs/02-screens.md, docs/03-release-checklist.md → UX + packaging
If you invent anything not covered there, STOP and ask me a numbered question first.
Do NOT use the original app's servers/branding. Do NOT copy any proprietary code — this repo
contains a spec, you write 100% original implementation.

HARD CONSTRAINTS (non-negotiable)
- applicationId "com.sobrr.agnes" is FORBIDDEN (original id); use "com.example.companion" +
  own app name. minSdk 26, targetSdk 34, Kotlin 2.x, Gradle version catalogs, ksp.
- Backend base URL must be configurable per flavor (dev/stage/prod) via BuildConfig; provide a
  MOCK backend toggle (in-memory fake implementing every endpoint of README §5, incl. SSE).
- NEVER hardcode secrets. The Agnes AI gateway key lives ONLY in the optional backend sidecar
  (env AGNES_API_KEY / AGNES_BASE_URL). The Android app must run 100% in MOCK mode by default.
- All AI calls go client → YOUR backend sidecar → gateway (apihub.agnes-ai.com/v1). Models:
  agnes-2.5-flash (chat/stream/tools), agnes-image-2.1-flash (image), agnes-video-v2.0 (video,
  poll via video_id). Obey docs/04 §1.1 routing + §6 retry matrix (backoff only on
  408/429/500/502/503/504/520/522/524) and error→UX mapping.
- Chat stream is SSE with typed blocks: thinking / skill_load / tool_call (ToolCallEnum:
  LoadSkill, GenerateImage, WebSearch, ImageSearch, Read/Write/EditFile, ListFiles, Execute,
  WriteReport, QueryWeather, ProfileData, Other) / text / image / artifact / followups / error /
  done — render them as Compose "design-system" components (skill card = SkillLoadedDisplay,
  thinking = expandable accordion).
- Envelope: BaseResponse<T>{code,message,data} + ApiResult call adapter; pagination
  PaginationInfo; uploads = two-step presigned-url → PUT → complete/process.

LOCKED TECH STACK
Kotlin coroutines+Flow · Retrofit2+OkHttp3+Gson · Jetpack Compose (Material3) single-activity
+ Navigation Compose · Hilt DI · Room (3 filters/photo-picker/game DBs) · DataStore (community,
game, credits caches) · Media3 ExoPlayer (TTS voice) · Coil · Firebase Messaging (push, optional
behind flag) · WebView shell for H5-like pages · OkHttp SSE for streaming · JUnit5/Truth/Turbine
+ MockWebServer for tests. Provide a tiny backend sidecar: Kotlin Ktor (or Node Express) with
the MOCK implementation + Agnes-gateway proxy module.

MILESTONES (each must compile, run, and pass its smoke test before the next)
 M0  Repo scan report: list every doc fact you will implement (endpoints count, models, blocks)
 M1  Scaffold: modules :app :core-data :core-ui :feature-* ; flavors; BuildConfig envs; CI-less
     gradle build green on empty shells.
 M2  Data layer: envelope+ApiResult, ALL Retrofit services & DTOs from README §5 (90+ bindings),
     interceptors (auth token, security/parameter stub), Room DBs, DataStore stores, MOCK
     backend (Ktor) serving fixture data for every endpoint + SSE fixture stream.
 M3  Auth: email/phone/Google UI + session store, bind-email/phone, token refresh loop, logout.
     Smoke: login against MOCK, rotate refresh token.
 M4  Agent chat core: conversation list (search, running badge), SSE client (state machine
     with reconnect/resume/cancel), block renderer (thinking accordion, SkillLoadedDisplay,
     tool-call cards, artifacts chips), regenerate/resume/cancel/hitl-resume actions,
     TTS toggle (Media3), follow-up suggestions. Smoke: scripted fixture stream replays the
     EDIT-IMAGE chain (LoadSkill×3 → GenerateImage → image block) — assert UI state per block.
 M5  Attachments & artifacts: photo picker → presigned upload flow → artifact list/download/
     share; PPT viewer (page images) + website preview pane. Smoke: upload→artifact roundtrip.
 M6  Image studio: text→image and edit-image (reference image + prompt) using tool_mode
     "text_edit_image"; visuals/materials library, watermark-removal toggle, OCR screen.
 M7  Community + News: feed CRUD, emoji reacts, comments; news prefs/categories/read records.
 M8  Games + IM stub: catalog/category-tree, detail, create/join/quit groups, share-code join;
     chat group screen (Tencent IM behind interface; MOCK impl allowed).
 M9  Economy: tasks+points ledger, referral (code/redeem/stat), subscription plans/pricing,
     credits packs/balance, upgrade-gate dialogs (e.g. PPT), MOCK purchase verify v1+v2.
 M10 Hardening: push (FCM optional flag), version-check & migration banners, filters settings,
     dark theme, error/empty states per docs/04 §6.4, R8 release AAB (arm64-v8a/en/hdpi) build.

PLAN-MODE OUTPUT CONTRACT (your FIRST answer, before coding)
 1. Confirmed fact sheet extracted from the repo (counts: endpoints, DTOs, models, blocks).
 2. Module+package tree you will create.
 3. Table Milestone → files/classes → test command → expected result.
 4. Numbered OPEN QUESTIONS (only blocking ones) + your default answer each (I can override).
 5. Risks & assumptions (≤8 bullets).
 Then STOP and wait for "APPROVED".

OPERATING RULES DURING BUILD (critical for Deepseek-v4-Flash class models)
 - Work milestone-by-milestone; produce a brief ✅ STATUS note per finished milestone.
 - One file per tool action; after each file, re-state where it fits (1 sentence).
 - Maintain DECISIONS.md (why-library/format choices) and PROGRESS.md (milestones checkboxes).
 - If context runs low: re-read README §5 + the doc relevant to the CURRENT milestone only
   (never paste whole repo at once).
 - Every network model mirrors its DTO (Gson @SerializedName); every screen has a preview or
   screenshot test; every use-case has ≥1 unit test; total `gradle test` must pass.
 - Never silently change an endpoint, model name, or block type — always flag with "SPEC-DEVIATION:".
 - German UI labels allowed as fallback, but all resources in English (values/) + German
   (values-de/) where strings exist.

DEFINITION OF DONE
`gradle :app:assembleRelease` builds AAB with the 3 splits; `gradle test` green; app runs fully
against MOCK backend incl. M4 fixture replay; README §12 acceptance checklist reproducible.

FIRST ACTION NOW
Clone the repo, read README.md → docs/01 → docs/04 → docs/05, then output the PLAN-MODE
CONTRACT (section above). Do not write any code yet.
```
---

## Kurz-Anleitung (Deutsch)
1. Kilo Code öffnen → Mode **Plan** wählen → Modell **Deepseek-v4-Flash** aktivieren.
2. Den kompletten Block zwischen den ```..``` oben einfügen (inkl. Repo-URL).
3. Agent erstellt zuerst den **Plan-Vertrag** (Faktenblatt, Modulbaum, Meilenstein-Tabelle,
   Fragen) → du prüfst → mit **APPROVED** startet der Build M0–M10.
4. Empfohlen: pro Meilenstein einen eigenen Kilo-Task; `PROGRESS.md`/`DECISIONS.md` führt der
   Agent selbst, daran erkennst du den Fortschritt.
5. Live-Betrieb später: MOCK umstellen auf echtes Backend + `AGNES_API_KEY` (nur Server-seitig,
   nie im APK) — siehe docs/04.
