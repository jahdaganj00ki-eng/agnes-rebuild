# Tiefen-Analyse: Einstellungen, Endpunkte, Modelle & Konfiguration der Original-App
**App:** Agnes `com.sobrr.agnes` v3.0.61 (3000061) · minSdk 26 / targetSdk 36 · Analyse 2026-08-02
**Methodik:** Dekompiliert ~9.134 Klassen + vollständige Ressourcen (Manifest, strings.xml, Splits)

---

## 1. Umgebungen & Domains (Environments)

| Rolle | Produktiv | Staging/Test | Zweck |
|---|---|---|---|
| App-API | `https://api.agnes-ai.com` | `api-preview.agnes-ai.com` · `api-agnes-dev/test.kiwiar.com` | REST mit `BaseResponse<T>`-Envelope |
| H5/Web | `https://app.agnes-ai.com/` | `preview.agnes-ai.com` · `beta.agnes-ai.com` · `app-agnes-{dev,test}.agnes-dev.com` | WebView-Produktseiten |
| GCP-Test | – | `agnes-test-gcp.kiwiar.com` (auch Deep-Link-Host) | Infra-Preview |
| Storage/CDN | – | `cos-aigc-default-test.kiwiar.com` (≈ Tencent COS) | AIGC-Bilder/Assets |
| Deep-Links | `app.agnes-ai.com` (**Pfade `/detail`, Prefix `/filter/`**), Scheme `agnes://`, `market://`, `pushscheme://` | Hosts dev/test wie oben | + **Singular-OneLink `agnes.sng.link`** |
| KI-Gateway (nur Backend) | `https://apihub.agnes-ai.com/v1` (intl) · `apihub.agnes-ai.cn/v1` (alt) · `api.agnes-ai.cn/v1` (CN) | – | OpenAI-kompatibel; **nicht** im Client |
| Versionierung | `BUILD_TYPE release`, `FLAVOR googleplay`, `GOOGLE_SERVICES_ENABLED true` | | BuildConfig |

## 2. HTTP-Client & Netzwerk-Einstellungen

- Stack: Retrofit2 + OkHttp3 + Gson; eigener `ApiResult`-Call-Adapter · Envelope `BaseResponse<T>`, `BaseNoResponse`; Pagination `Pagination/PaginationInfo`
- **Timeouts:** connect **60 s**, read **60 s** (Stream-Calls **120 s**), write **120 s** — eingestellt auf lange Livedauer der SSE-Chat-Streams
- **Header (gefunden):** `platform` (mehrfach), `x-dev-lane` (Environment-Lane/Umschalter), `x-small`/`x-large` (Bild-Größenvarianten am Storage), `X-TC-Token` (Tencent-Cloud), Firebase/Crashlytics-Standardheader; Auth via OkHttp-Interceptor (Bearer-Rotation über `/api/v1/user/refresh-token`)
- **Signierung:** sensibles Anti-Abuse-Paket via `POST /api/v1/security/parameter`; Upload-Token werden clientseitig **entschlüsselt** (`FileUploadManager.decryptToken`)
- Uploads: immer 2-Schritte — presigned-url → PUT → complete/process; Multipart init/complete/abort für große Dateien

## 3. API-Endpunktkarte (echte Retrofit-Bindings — ~90)

Kernfamilie **`/api/v1/agnes/*`** (Agent): `chat/stream` (+ `cancel`/`resume`/`regenerate`/`hitl-resume`), Conversations CRUD (GET/POST/PATCH/DELETE), `history`, `search`, `running`, `title-summary`, `agnes-chats`, `follow-up-questions`, `mode_support_models`, `daily-hot-topics/latest`, `recommend-topics(+refresh)`, `ai_voice/tts-toggle` (GET+POST), `image_ocr`, `ppt/upgrade-gate/check`, `website/publish`, `user/materials`, `user/visuals`, Artifacts (`…/by-id/{id}/artifacts/list`, `…/artifacts/{eventId}/download`, `artifact-share`)

Weitere Familien:
- **Auth/User:** `auth/token_by_email`, `auth/me` (GET/DELETE), `auth/clear_firebase_token`, `auth/update_user_{avatar,name}`, `user/login`, `register`, `refresh-token`, `code/send`, `code/verify`, `bind_email`, `bind_phone`, `reset_password`, `timezone`, `profile` (PUT v1 / GET v2), `user/account` (DELETE), `fcm/token`, `migration/waiting`, `version/check`
- **Files:** `file/presigned-url` (+v1), `file/multipart/{init,complete,abort}`, `file/process/{avatar,chat}`, `user/avatar/presigned-url`, `file/conversation/{sandbox,uploads}`, `aigc/user-assets`, `visuals/remove_watermark`
- **Templates/Charaktere:** `template/categories`, `list_by_category`, `list_by_ids`, `drafts` (CRUD), `generate`, `upload`, `like` + `aigc/template/drafts/create|list`, `search`, `collect|uncollect|dislike/{template_id}`, `group_stencil/stencil_detail/{id}`
- **Games/IM:** `game/list`, `list-by-show-type`, `catalog/category-tree`, `games/{game_id}`, `info/search`, `game-name-map`, `user-assets`, `profile/{userId}`(GET)+update(POST), `game/groups`(POST)+`join`/`quit`/`info`/`active-by-game`, `help-answer`, `im/user-sig`, `im/group-share-codes/{shareCode}`, `im/groups/{groupId}` (DELETE)
- **News:** `news/list_with_custom`, `detail`, `search`, `user-categories` (GET/PUT), `record-read`, `read-stats`, `recent-reads`, `clear-reads`, `preference`
- **Billing:** `v2/subscription/plans`, `v2/subscription/credits-balance`, `v1/subscription/credits-packs`, `credits-transactions`, `verify-payment`, **`/v2/purchase`**, `v1/subscription/cancel`
- **Referral:** `invitation/config`, `code`, `code/{code}/redeem`, `log`, `stat`
- **Community:** DTO-gebunden (Post/Emoji/Comments) — Pfade über konstanten Upstream, nicht annotiert gefunden

## 4. Genutzte KI-Modelle (Beleglage)

| Ebene | Befund | Konfidenz |
|---|---|---|
| Client kennt Modell-**Katalog** | DTO-Felder `model_code`, `model_alias`, `model_type`, `is_online`, `subscription_level` + Endpunkt **`mode_support_models`** (Modell-Liste je Szene) | sicher |
| Statischer Fallback im Client | genau **ein** hartkodierter Katalog-Eintrag: Code **`agnes-image`** (Anzeige „Agnes Image", chinesische Kurzbeschreibung, COS-Beispielbild) | sicher |
| Inferenz | **kein** Direktaufruf eines Modell-Gateways im Client; geschieht serverseitig hinter `api.agnes-ai.com` | sicher |
| Modell-Familie | `agnes-*-flash` Familie (siehe Anbieter-Repo/Eigenkatalog) — semantisch konsistent | groß |

## 5. Drittanbieter-SDKs & Keys (App-seitige Konfiguration)

| SDK | Konfiguration/Belege |
|---|---|
| **Firebase** | `project_id agnes-457208`, `google_app_id 1:…:android:…`, `gcm_defaultSenderId 1091283270456`, `default_web_client_id …apps.googleusercontent.com`, Storage-Bucket `agnes-457208.firebasestorage.app`, Google-`api_key` in strings.xml (öffentliche Client-Konfiguration, wie bei jeder Firebase-App) |
| **Tencent IM + Push** | Paket `com.tencent.qcloud.tim.push*` (TIMPush, TUICore, ServiceInitializer); UserSig vom Backend (`/api/v1/im/user-sig`); Push-Vendor-Kanäle über Manifest (24 Meta-Daten-Einträge) |
| **Attribution** | **Singular** aktiv: OneLink-Host `agnes.sng.link`, Prefs `singular-pref-session`, `pref-singular-id`, `singular-licensing-api`, `batch_send_id`; zusätzlich Spuren von **Adjust** (Manifest-Provider + `adjust_keys` Prefs) und **AppsFlyer** (`appsflyer-data` Prefs) → Stack mit Singular als aktivem Link-System |
| **Play Billing** | Subscriptions + One-Time (Credits/Fuel Packs), Verify v1 **`verify-payment`** + **v2 `/v2/purchase`**, Cancel-Binding |
| **Play Core / Credentials** | In-App-Update-Dialoge, Google-One-Tap via Credential Manager |
| Weitere | Room3, DataStore, media3, profileinstaller, startup, emoji2, window, RecyclerView/ConstraintLayout, **Compose inkl. `org.jetbrains.compose`-Resources** |

## 6. Clientseitige Einstellungen & Preferences

| Thema | Befund |
|---|---|
| Eigene Pref-Datei | **`a8f2b9c4d7e1`** (verschleiert benannte SharedPreferences-Datei → App-Owner); `key_prefix`-Pattern für verschachtelte Keys |
| Teen-Modus | komplette UI: `setting_teen_mode`, Passwort setzen/zum Deaktivieren nötig, Hinweis „strikteres Filtering der Ein-/Ausgabe" (Inhaltsfilter `feature_filters` + Room `AgnesFilterDatabase`) |
| Voice | `ai_voice/tts-toggle` Remote-Einstellung; Strings `voice_call_ended`, „voice call creation failed" → **Voice-Call-Ebene zusätzlich zu TTS** |
| Account | `account_settings`, Deletion-String referenziert **Produktfamilie „Sapiens": agnes, pixa, echo, volt** (→ Betreiber-Portfolio; Pixa = Layout-System im Code) |
| WeChat | `chat_on_wechat` (Support-Kanal) · Benachrichtigungen: `notification_permission_not_enabled` |
| Debug | Schalter-String `agnes.debug.mode` |
| Sonder-Strings | `thinkingExpandMap` (Thinking-Akkordeon), `tool_mode=text_edit_image`, `agnes_main_chat_`, `agnes_prefs`, `agnesChatStatus` |

## 7. Ressourcen/UI-System

- **Nur englische Res** im Bundle (`config.en`-Split); konfigurationsgetrieben: 25 `values-*` Varianten (hdpi/sw600dp/watch/night/v28-v35…), **Compose-Ressourcen**; Server-driven Layouts via „Pixa"-Modelle (Pages/Modes/Features/Staggered-Grids)
- App Icon 1024×1024 aus XAPK; Basis-AAB ~31 MB; Splits: `arm64_v8a + en + hdpi`
- Push: FCM FireBaseMessagingService `FcmService` + FirebaseInstanceIdReceiver + TIMPush-Kanäle (OEM-Push)

## 8. Konsolidiertes „Settings-Profil" des Rebuilds

| Kategorie | Exakte Werte für den Rebuild (aus Spec „agnes-rebuild") |
|---|---|
| Netzwerk | 60/60–120/120 s Timeouts · Bearer-Interceptor · `x-dev-lane`-Env · OAuth-Refresh via `/refresh-token` |
| Streaming | SSE ≥120 s fähig, Blöcke thinking/skill_load/tool_call/text/image/artifact/followups/error/done |
| Modelle (Backend) | Mapping per `mode_support_models` kuratieren; Codes/Metadaten generisch; Anbieterseite `agnes-2.5/2.0/1.5-flash`, `agnes-image-2.1/2.0-flash`, `agnes-video-v2.0` |
| SDK-Äquivalente | Firebase (FCM+Analytics) · Tencent-IM-Naht via Interface + `im/user-sig` · Attribution: Singular-Schnittstelle optional hinter Flag |
| Feature-Defaults | TTS-Toggle default ON · Teen-Modus mit Eltern-PIN · Version-Check-Banner · Migration-Banner · FeatureConfig {category,mode,type} · LoginConfig {default_page} |
| Unverzichtbare Pflicht-Endpunkte | README §5 vollständig; Priorisierung: chat/stream-Set > auth/user > file > billing-v2 > im |
| Rechtssafe Abweichungen | eigene Package-ID/branding; Original-Keys (Firebase-Projekt etc.) **nicht** übernehmen — eigenes Firebase-Projekt, eigene Buckets |

*Datenlage: alle Angaben aus öffentlich zugänglicher Spezifikation + eigener faktischer Struktur-Analyse; Firebase-/Google-IDs sind Standard-Client-Konfigurationen und sind kein geheimer Zugang.*
