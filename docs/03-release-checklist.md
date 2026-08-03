# Release / Build Configuration Checklist

- `applicationId` `com.sobrr.agnes`, `minSdk 26`, `compileSdk 36`, `targetSdk 36`,
  `versionCode` scheme `3.00.0xx` → e.g. `3000061` = `3.0.61`.
- Product flavors per environment (dev / test / preview / prod) injecting `BASE_URL` and
  `H5_URL` (values in README §2) + per-flavor `google-services.json`.
- Dependencies (Gradle version catalog): Kotlin coroutines, Retrofit2, OkHttp3(logging),
  Gson, Room3 runtime+compiler(ksp), DataStore preferences, androidx.lifecycle/credentials,
  compose BOM (chat UI), media3 (exo player/ui), Play Billing KTX, Play Core, Firebase BOM
  (analytics, messaging, sessions, crashlytics optional), Tencent IM + TIMPush, Adjust,
  AppsFlyer, androidx start-up / profileinstaller / emoji2 / window.
- Enable R8 (full mode) for release; package as **AAB**; abi split arm64-v8a only,
  language split `en`, density `hdpi` (mirrors original split config).
- Manifest components: `AgnesApplication`, `MainActivity` (single task), `FcmService`
  (FirebaseMessagingService), `FileProvider`, Firebase/Tencent push plumbing, Billing proxies,
  Adjust provider, Room3 multi-instance invalidation, startup/profileinstaller providers.
- Permissions: INTERNET, CAMERA, RECORD_AUDIO, READ_MEDIA_IMAGES, READ_MEDIA_VIDEO,
  READ_EXTERNAL_STORAGE (maxSdk 32), POST_NOTIFICATIONS, VIBRATE, WAKE_LOCK,
  ACCESS_NETWORK_STATE, AD_ID, BILLING, C2DM RECEIVE, install-referrer, AD_SERVICES_*.
- Original package signature total ~31 MB AAB with `xapk` v2 layout (base + 3 splits).
