# SKILLS-RECHERCHE: Agent-Skills in der Original-App „Agnes"
**Stand 2026-08-02 · Quellen: APK-Strukturanalyse (9.134 Klassen) + öffentliche AgnesAI-Labs-Repos + Skill-Ökosystem-Recherche**

---

## 1. Kernaussage

Die Skills „image-generation", „image-generation/reference-image" und „image-prompt-craft" sind
**agent-side Skills** im **SKILL.md-Format** (Verzeichnis-Skills mit Unter-Referenzen, wie es
Claude Code / Codex / OpenClaw verwenden). Sie leben **nicht** im APK und auch nicht im H5-Bundle,
sondern werden vom **serverseitigen Agent-Runtime** geladen. Der Client zeigt das Laden nur an.

**Belegkette im APK:**
- Der Chat-Stream trägt die Parameter `tool_mode`, `scene`, `agent_type`.
  Beim „Edit Image"-Flow sendet der Client **exakt**: `tool_mode = "text_edit_image"` (ebenso `image_gen_prompt`).
- Die enum **ToolCallEnum** (feature_task) listet jeden Tool-Call-Typ, den der Agent-Loop ausführen
  kann — darunter **`LoadSkill` (= der sichtbare „Skill wird geladen"-Schritt!)** und **GenerateImage**.
- Die UI-Komponente **`SkillLoadedDisplay`** rendert den geladenen Skill-Namen im Chat; die
  ausklappbaren „Thoughts/Thinkings" sind der UI-String **`thinkingExpandMap`**.

## 2. Bestätigte Skill-Kette im „Edit Image"-Flow

| Reihenfolge | Skill | Aufgabe (rekonstruiert) |
|---|---|---|
| 0 | *(Denkphase – `thinkingExpandMap`)* | Agent plant: Upload analysieren → Referenz einziehen → Prompt verfeinern → generieren |
| 1 | **`image-generation`** | Haupt-Skill für KI-Bilder: ruft das Bild-Modell (im Ökosystem: `agnes-image-2.x-flash`, `POST /v1/images/generations`) |
| 1.1 | **`image-generation/reference-image`** | Unter-Referenz des Skills (Datei `reference-image` im Skill-Verzeichnis): Verarbeitung des hochgeladenen Referenzbilds — Stil/Inhalt/Maske-Übernahme, Bild-zu-Bild-Edit |
| 2 | **`image-prompt-craft`** | Prompt-Engineering-Skill: aus Nutzer-Eingabe + Bildkontext einen optimierten Generierungs-Prompt bauen |
| 3 | *(Ausführung `GenerateImage`)* | das bearbeitete Bild wird erzeugt und als Asset zurückgeliefert |

Format-Hinweis: `skill-name` = Skill-Verzeichnis, `skill/sub-doc` = Referenzdokument darin —
identisch zur Struktur des öffentlichen Skills `AgnesAI-Labs/skills/agnes-ai-models`
(SKILL.md + `references/*`-Unterdokumente).

## 3. Alle in der App greifbaren Agent-Fähigkeiten (ToolCallEnum — vollständig)

| Tool-Call | Bedeutung | Wo sichtbar |
|---|---|---|
| **`LoadSkill`** | Skill-Verzeichnis + Referenzen in den Agent-Context laden (das „Lädt Skill…" in der App) | Edit-Image, Agent-Flows |
| `GenerateImage` | Bilderzeugung/-bearbeitung | Edit Image, Avatar-Generierung |
| `ReadFile` / `WriteFile` / `EditFile` / `ListFiles` | Datei-Werkzeuge auf einem serverseitigen Workspace | Agent-/Research-Flows |
| `Execute` | Code-/Kommando-Ausführung serverseitig | Agent-Flows |
| `WebSearch` / `ImageSearch` | Websuche / Bildersuche mit Quellen | Deep-Research-Style Antworten |
| `WriteReport` | strukturierter Bericht/Dokument-Erzeugung | Research-Flows |
| `QueryWeather` | Wetter-Tool | Chat-Tools |
| `ProfileData` | Zugriff auf Nutzerprofil-Daten im Agent-Kontext | Personalisierung |
| `Other` | Fallback | – |

OpenAI-kompatibilität: jeder Tool-Call trägt eine `tool_call_id` (Standard-Format der
Chat-Completions-Tool-Calls) — konsistent mit dem Gateway `apihub.agnes-ai.com/v1`.

## 4. Weitere Skill-/Szenen-Ebene (aus Feature-Modulen abgeleitet)

Über die generischen Tools hinaus existiert mindestens ein **domänenspezifisches Skill-Feature**:
- **PPT/Slides-Skill**: Felder `slides`, `slide_urls`, `slide_image_urls`, `slide_pages`,
  `download_ppt`/`pptx` + Endpunkt für die Abo-Prüfung `/api/v1/agnes/ppt/upgrade-gate/check`
  (Referenzklasse `PptUpgradeGateCheckResponse`) → Präsentationen erzeugen + herunterladen,
  freigeschaltet nach Plan.
- Szenen-Parameter (`scene`, `tool_mode`, `agent_type`) deuten auf mehrere Tool-Modi neben
  `text_edit_image` hin; nur dieser eine ist hartkodiert im Client, die übrigen werden
  server-konfiguriert (passt zu `ModeSupportModels*`/`FeatureConfig`).
- Zeichen-Erstellungs-Skills (builtin-Endpunkte): `GenerateCandidateImages`,
  `GeneratePersonalityBrief`, `GenerateOpeningLine`, `CreateRoleAvatar` — effektiv
  Prompt-Craft-Skills, serverseitig kuratiert.

## 5. Öffentlich vs. intern: Was gibt es auf GitHub?

| Skill | Öffentlich? | Ort |
|---|---|---|
| `agnes-ai-models` (Gateway-Integrations-Skill für Coding-Agenten) | ✅ ja | `AgnesAI-Labs/skills/agnes-ai-models` (SKILL.md + references/) |
| `image-generation`, `image-prompt-craft` u. ä. App-Skills | ❌ nicht gelistet | serverseitiger Skill-Store der Agnes-AI-Plattform; nur über das App-Verhalten (LoadSkill-Events) sichtbar. Kein offizieller öffentlicher Mirror auffindbar. |
| Generische Drittanbieter-Versionen (`image-gen`, `gpt-image`, claude-office `image-generation`) | ✅ ähnlich | Skill-Registries — **gleiches Format**, **nicht** die Agnes-Implementierung |

## 6. Konsequenz für den Rebuild

1. Rebuild-Architektur braucht einen **Skill-Store** (Verzeichnis `skills/<name>/SKILL.md` +
   Unter-Referenzen, z. B. `reference-image`) + Agent-Loop mit Tool-Calls nach dem
   Chat-Completions-Tool-Format (`tool_call_id`).
2. UI-Blöcke nachbauen: Thinking-Akkordeon (`thinkingExpandMap`-Äquivalent),
   `SkillLoadedDisplay`-Block, Tool-Call-Karten je Enum-Typ (s. Tabelle §3).
3. Mindest-Skill-Set für Feature-Parität: `image-generation` (+ `reference-image`),
   `image-prompt-craft`, `slides/ppt` (+ Upgrade-Gate), `web-search`, `image-search`,
   `file-tools`, `execute`, `report-writer`, `weather`, `profile`.
4. Wichtig: Nutzer-Upload → serverseitige Datei-Ablage (presigned URLs, Upload-Modul der
   Spec) → Pfad/URL geht als `reference` in den Skill — exakt der beobachtete Edit-Image-Start.

*Methodik-Hinweis: Fakten aus öffentlichen Repos und eigener Verhaltens-/Strukturanalyse;
keine Original-Skill-Inhalte (intern serverseitig, nicht öffentlich zugänglich).*
