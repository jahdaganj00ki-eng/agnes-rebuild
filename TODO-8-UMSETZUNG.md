# Umsetzung von Abschnitt 8: Konkrete To-do-Liste

Quelle: `/home/user/gegenpruefung-agnes-rebuild-vs-komplettanalyse.md`, Abschnitt **8. Konkrete To-do-Liste für das Repo**.

## Status

| # | To-do | Status | Umsetzung |
|---:|---|---|---|
| 1 | `docs/04-ai-gateway.md` mit präziseren Angaben aktualisieren | ✅ erledigt | Datei vollständig überarbeitet: Gateway-Konfig, Modelle, Requests, Limits, Fehler/Retries |
| 2 | Retrofit-Beispiel korrigieren: kein doppeltes `/v1`; separater Root-Client für Video-Polling | ✅ erledigt | `docs/04-ai-gateway.md` §2 enthält `AgnesGatewayV1` + `AgnesGatewayRoot` |
| 3 | Modellrouting-Tabelle ergänzen: App-`model_code` → Provider-Modell | ✅ erledigt | `docs/04-ai-gateway.md` §4.2 und `backend-sidecar/model-routing.yaml` |
| 4 | Bildparameter korrigieren: `size` als Tier, `ratio`, `extra_body.response_format` | ✅ erledigt | `docs/04-ai-gateway.md` §5.2 und `docs/01-api-surface.md` |
| 5 | Video-Polling prominent festschreiben: `video_id`, nicht `task_id` | ✅ erledigt | `docs/04-ai-gateway.md` §5.3, `README.md` §8, `docs/01-api-surface.md` |
| 6 | Rate-Limit-Tabellen aus der MD übernehmen, inklusive Key-Pool-Hinweis | ✅ erledigt | `docs/04-ai-gateway.md` §7 |
| 7 | Android-Gradle-Zielwerte prüfen: `compileSdk/targetSdk` ggf. auf 36 setzen | ✅ erledigt | `app/build.gradle.kts` auf 36/36 geändert; `docs/03-release-checklist.md` angepasst |
| 8 | Backend-Sidecar als neues Projekt/Modul ergänzen | ✅ erledigt | Neuer Ordner `backend-sidecar/` mit README, `.env.example`, Routing-YAML, Kotlin-Contract-Skizze |
| 9 | Secrets nur per Env/Secret-Manager, nie in Repo/Android-App | ✅ erledigt | `.env.example` ohne Real-Key; `.gitignore` schützt `.env`; Doku-Warnungen ergänzt |
| 10 | Nach externem Teilen des Keys: Key in der Agnes-Plattform rotieren | ⚠️ Hinweis dokumentiert | Nicht im Repo automatisierbar; bitte in der Agnes-Platform-Console manuell rotieren |

## Geänderte / neue Dateien

- Geändert: `.gitignore`
- Geändert: `README.md`
- Geändert: `app/build.gradle.kts`
- Geändert: `docs/01-api-surface.md`
- Geändert: `docs/03-release-checklist.md`
- Geändert: `docs/04-ai-gateway.md`
- Geändert: `docs/06-app-configuration.md`
- Neu: `backend-sidecar/README.md`
- Neu: `backend-sidecar/.env.example`
- Neu: `backend-sidecar/model-routing.yaml`
- Neu: `backend-sidecar/src/main/kotlin/com/agnes/sidecar/AgnesGatewayContracts.kt`
- Neu: `TODO-8-UMSETZUNG.md`

## Offener manueller Punkt

Der im Chat offengelegte API-Key sollte in der Agnes-Plattform rotiert werden. Er wurde nicht in
Dateien geschrieben und nicht für Test-Requests verwendet.
