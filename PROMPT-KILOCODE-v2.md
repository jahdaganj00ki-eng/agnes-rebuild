# KILO CODE — PLAN MODE MASTER PROMPT v2 (Rebuild + Agnes-AI-Models Gateway integriert)
# Kopiere alles unterhalb der Linie in den Plan Mode.

---
```text
ROLE
You are a senior Android architect + Kotlin engineer running in PLAN MODE on the model
"Deepseek-v4-flash". PLAN FIRST, then wait; only code after I reply "APPROVED".

MISSION
Build a complete Android AI-companion app ("Agnes-style super-app") from the spec repo
https://github.com/jahdaganj00ki-eng/agnes-rebuild  —  INCLUDING a production-grade backend
sidecar that integrates the Agnes AI gateway exactly as documented for
AgnesAI-Labs/AgnesAI-Models (OpenAI-compatible API):
base https://apihub.agnes-ai.com/v1 · routes apihub.agnes-ai.cn/v1 (alt) / api.agnes-ai.cn/v1
(CN) · models agnes-2.5-flash, agnes-2.0-flash, agnes-1.5-flash, agnes-image-2.1-flash,
agnes-image-2.0-flash, agnes-video-v2.0 · video polling GET /agnesapi?video_id=.
Deliverable = Android app + :sidecar (Kotlin Ktor) + MOCK mode that needs NO real key.

SINGLE SOURCE OF TRUTH (precedence)
 1) README.md (§5 real endpoint bindings, §7 agent/skill runtime, §8 provider)
 2) docs/04-ai-gateway.md  (gateway routes, model table, scene→model mapping, quotas, RPM,
    error-code matrix §6.4 — treat as normative)
 3) docs/01-api-surface.md (DTO wiring, SSE block contract)
 4) docs/05-agent-skills.md + SKILLS-RECHERCHE.md (skills, tool_mode, Edit-Image chain)
Anything missing → numbered question first; never invent endpoints/models.

HARD CONSTRAINTS
- Original package id/branding forbidden → use "com.example.companion". minSdk 26/target 34.
- Secrets ONLY in :sidecar env (AGNES_API_KEY, AGNES_BASE_URL). APK ships ZERO keys.
- Default run profile = MOCK (in-memory implementation of every README §5 endpoint incl. SSE).
  Profile LIVE = sidecar proxies to the real gateway. Switch = single BuildConfig/flag.
- Model registry + scene map is DATA (models.json in sidecar), not code — allows hot updates:
  chat/stream→agnes-2.5-flash · title-summary, follow-ups→agnes-1.5-flash · persona/opening-line,
  game help→agnes-2.0-flash · avatars/edit-image→agnes-image-2.1-flash · OCR/vision→2.5 image-url
  input · video moments→agnes-video-v2.0 (poll by video_id, never task_id).
- SSE block protocol (client contract, unchanged): thinking / skill_load / tool_call
  (ToolCallEnum: LoadSkill, GenerateImage, WebSearch, ImageSearch, Read/Write/EditFile,
  ListFiles, Execute, WriteReport, QueryWeather, ProfileData, Other) / text / image / artifact /
  followups / error / done. tool_calls carry OpenAI-style tool_call_id.
- Gateway rules (docs/04 §1.1 & §6): no double /v1; never route-hop on 4xx; exponential backoff
  ONLY for 408/429/500/502/503/504/520/522/524; quotas=metered per access type
  (image by count, video by seconds, text per request); 402→upgrade-gate UX mapping.
- 100% original code (this repo is a spec; no proprietary material may be reproduced).

LOCKED STACK
Android: Kotlin coroutines/Flow · Compose M3 single-activity · Hilt · Retrofit2+OkHttp+Gson ·
Room · DataStore · Media3 · Coil · OkHttp-SSE · JUnit5/Turbine/MockWebServer.
Sidecar: Kotlin Ktor server (Netty), kotlinx-serialization, env-based config, in-memory mock
stores, /healthz; docker-compose.yml optional.

MILESTONES (M0–M11; each compiles + passes its smoke gate before proceeding)
 M0  Spec audit: counts+list (endpoints, DTOs, models, blocks, skills, routes) from the repo.
 M1  Scaffold: :app :core-* :feature-* :sidecar ; flavors dev/stage/prod; MOCK/LIVE flag; CI-less
     gradle build green.
 M2  :core-data complete: envelope+ApiResult; ALL Retrofit services+DTOs (90+ bindings, README §5);
     interceptors; Room DBs; DataStore stores; Ktor MOCK server covering every endpoint + SSE
     fixture streams (incl. the Edit-Image chain replay).
 M3  ⭐ GATEWAY SIDECAR (core of this task):
     3.1 config: env AGNES_API_KEY/AGNES_BASE_URL, route table (intl/intl-alt/cn), route rules
     3.2 OpenAI-compatible client implementing the FULL request contracts of docs/04 §2.1–2.3:
         chat/completions (SSE; params incl. tools/tool_choice, chat_template_kwargs, thinking)
         + /v1/responses for 2.5-flash · images/generations with tiers size 1K-4K + ratio list,
         image[] multi-input, and extra_body.response_format (never top-level) · videos with
         num_frames 8n+1 ≤441, tier normalization 480/720/1080, video_id-only polling (~5 s),
         retry matrix 408/429/5xx/520/522/524 + jitter
     3.3 stream converter: gateway deltas → block protocol (thinking/skill_load/tool_call/…)
     3.4 agent loop + skill store (sidecar-local SKILL.md dirs):
         skills/image-generation (+reference-image doc), skills/image-prompt-craft,
         skills/slides, skills/web-search; function-calling loop emits tool_call blocks in the
         ToolCallEnum forms incl. LoadSkill; Edit-Image tool_mode="text_edit_image" triggers
         the chain: LoadSkill(image-generation) → LoadSkill(reference-image) →
         LoadSkill(image-prompt-craft) → GenerateImage (reference=upload URL)
     3.5 quota ledger: per-access-type RPM throttles + plan quotas (tables docs/04 §6.2/§6.3),
         402 mapping to upgrade-gate response used by the app's gate check endpoints
     3.6 contract tests (MockWebServer, recorded fixtures — NO live key): chat SAR per model,
         image gen, video create+poll sequence, each retry code path, route-switch cases,
         error→envelope mapping per §6.4. Gate: `gradle :sidecar:test` all green.
 M4  Auth flows (3 providers + bind + refresh loop) against MOCK; token storage.
 M5  Agent chat screen: list(search/running), SSE state machine, Compose block renderers
     (accordion thinking, SkillLoadedDisplay, tool cards, artifact chips, followups),
     regenerate/cancel/resume/hitl-resume, TTS toggle. Gate: M2 fixture replay passes UI tests.
 M6  Uploads & artifacts: picker → presigned 2-step → process/complete; artifact list/download/
     share; PPT page viewer; website preview + publish call.
 M7  Image studio: text→image, edit-image (tool_mode text_edit_image + reference upload),
     visuals/materials libs, watermark toggle, OCR.
 M8  Characters & templates: creation wizard (avatar candidates → 2.0-flash persona → opening
     line → publish), template gallery like/collect/drafts.
 M9  Community+News: feed CRUD/emoji/comments; news prefs/records.
 M10 Games & IM stub: catalog/tree/detail, groups create/join/quit, share-code join, group chat
     behind IM interface (MOCK impl; Tencent-IM seam documented).
 M11 Economy & hardening: tasks/points ledger, referral code/redeem/stat, plans/pricing/credits
     packs/balance, upgrade gates (PPT etc.), purchase verify v1+v2 (MOCK), push flag, filters,
     version/migration banners, dark theme, R8 → AAB splits arm64-v8a/en/hdpi.

PLAN-MODE CONTRACT (first reply, no code)
 1) Fact sheet from M0 (verbatim counts vs repo)  2) module/package tree incl. :sidecar layout
 3) milestone→files→test-cmd→expected table  4) numbered blocking Qs + your default answers
 5) risks ≤8  → then STOP, wait for APPROVED.

BUILD OPERATING RULES (Deepseek-v4-flash tuning)
- Milestone-locked work; ✅ STATUS per milestone; one file per action + 1-line placement note.
- Keep PROGRESS.md + DECISIONS.md updated (they are your memory between tasks).
- Low context? Re-read ONLY README §5 + the active milestone's doc.
- Spec change = prefix "SPEC-DEVIATION:" with reason + ask.
- Every service/DTO mirrors the spec name (@SerializedName); ≥1 unit test per use-case;
  UI replay test for M5; `gradle test` must stay green; sanity: no model string may hardcode
  outside models.json.

DEFINITION OF DONE
gradle :app:assembleRelease (3 splits) + `gradle test` + `gradle :sidecar:test` green · app
runs fully in MOCK incl. Edit-Image replay · LIVE profile documented (steps to enable with a
real AGNES_API_KEY, quota tiers per docs/04 §6) · acceptance checklist README §12 reproducible.

FIRST ACTION NOW
Clone the repo; read README.md → docs/04 → docs/01 → docs/05; then OUTPUT THE PLAN-MODE
CONTRACT. No code yet.
```
---

## Kurz-Anleitung (Deutsch)
1. Kilo Code → **Plan**-Modus → Modell **Deepseek-v4-flash**.
2. Block in den Backticks oben komplett einfügen.
3. Agent liefert den Plan-Vertrag → prüfen → `APPROVED` → Build M0–M11.
4. **Unterschied zu v1:** M3 baut jetzt die komplette Agnes-Gateway-Integration als eigenes
   Meilenstein-Ziel (Modell-Registry via models.json, Skill-Store + Agent-Loop, Quota-Ledger,
   Contract-Tests ohne echten Key). MOCK bleibt Standard, LIVE ist dokumentierter Umschalter.
5. Für LIVE später: free Token-Plan passend wählen (docs/04 §6.3), Key nur Server-seitig.
