# Screens & Flows

Single-activity architecture around `MainActivity`; native screens + H5 WebView containers.

1. **Splash / bootstrap** — Firebase/notification permission primer (`POST_NOTIFICATIONS`),
   remote config (`FeatureConfig`, `GlobalErrorConfig`), session restore.
2. **Auth scene** (`AuthScene`-driven): tabbed Email / Phone / Google flows; registration;
   bind-additional-identifier screens; username change.
3. **Home shell** — bottom navigation: *Chat*, *Community*, *Games*, *Tasks*, *Me*.
4. **Chat** — conversation list (multi-conv management, search) → conversation screen:
   Compose message list, streamed block rendering (design-system blocks), preset replies,
   follow-up questions, regenerate; attachment entry via photo picker; audio via media3.
5. **Character creation wizard** — avatar candidates → personality brief → opening line →
   publish; projects/assets management screens.
6. **Community** — feed list; post detail with comments & emoji reactions; create-post editor.
7. **Games** — categories tree browser, game detail/intro, create-UGC-game, game group chat,
   join-by-share-code dialog, help Q&A.
8. **Tasks & Points** — task cards by type, points ledger with pagination, migration banner.
9. **Store / Billing** — subscription plans & pricing phases, credit/fuel packs, current
   subscription, quota/model-access explanations (what costs how many credits).
10. **Me/Settings** — profile, filters (content filter settings), notifications, H5 pages
    (help center, legal) in WebView; logout.

## Native ↔ H5 bridge
`MainActivity` hosts WebViews for the product pages served from the H5 base URL. The bridge
exposes: auth token, navigation intents, photo picker, upload, billing trigger, share, push
registration state. Rebuild it as a small `@JavascriptInterface` facade called `AgnesBridge`.
