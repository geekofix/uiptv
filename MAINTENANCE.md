# MAINTENANCE — Projektstatus und Arbeitsweise

Diese Datei ist die verbindliche Referenz für alle Sitzungen (auch neue/andere Chat-Sitzungen).
Sie wird bei jeder relevanten Änderung aktualisiert.

## Ziel des Projekts

Die **exakte Version v0.1.11 (Release 11, „Legacy-GUI")** von UIPTV behutsam weiterentwickeln:
neue Features, kleine Code-Verbesserungen, Bugfixes.

**Nicht** Ziel:

- kein Rebranding
- keine Übernahme der 1.x-UI/-Struktur von Upstream (die gilt als zu träge, mehr Klicks, unübersichtlich)
- **niemals** Merges vom Upstream-`main` (steht auf 1.1.1, neues Design)
- Versionen/Releases/Veröffentlichungen nur auf ausdrückliche Anweisung des Nutzers
- Repo-Sichtbarkeit nicht eigenständig ändern

## Basis und Branches

| Branch | Rolle | Regel |
|---|---|---|
| `baseline-0.1.11` | **BASIS** — exakt Tag `v0.1.11` (Commit `20660bf`) plus nur die pacman-Bau-Skripte | **Darf nie überschrieben, gelöscht oder inhaltlich verändert werden.** Keine Feature-Commits hier. |
| `dev-0.1.11` | **Standard- und Arbeitsbranch** — alle Weiterentwicklungen | Features/Fixes hier committen; basiert auf `baseline-0.1.11` |
| `main` | Upstream-Zustand (1.1.1, neues Design) | Nur Referenz. Nicht anfassen. |

Gelöschte Altlasten (2026-11-25): `legacy` und `legacy-dev` (Upstream-Legacy-Linie). Sie zeigten auf
KDE/Wayland einen leeren linken Navigationsbereich (Reiter wurden unsichtbar gezeichnet, Ursache nie
geklärt; auch JavaFX-Pin auf 26.0.1 half nicht). Nutzer-Entscheidung: zurück auf exakte 0.1.11-Basis.
Die Commits existieren weiterhin im Original-Upstream (xixogo5105/uiptv, Branch `legacy`) und im
Git-Verlauf, falls je ein einzelner Fix zurückportiert werden soll.

## Installation (CachyOS / Arch / pacman)

GitHub Actions baut bei jedem Push auf `baseline-0.1.11`/`dev-0.1.11` automatisch:

- **`UIPTV-0.1.11-pacman`** → natives Arch-Paket (`uiptv-…pkg.tar.zst`)
  - Installation: `sudo pacman -U uiptv-…pkg.tar.zst`
  - Deinstallation: `sudo pacman -R uiptv`
  - App: Menüeintrag „UIPTV" oder Befehl `uiptv`; Programm liegt in `/opt/uiptv`
- Relevant ist der Workflow **„Baseline 0.1.11 Arch package"** (`.github/workflows/baseline-arch.yml`).
- Downloads (Artifacts) laufen nach **1 Tag** ab (Einstellung `ARTIFACT_RETENTION_DAYS`). Falls abgelaufen:
Workflow per `workflow_dispatch` (Reiter Actions → Run workflow) neu anstoßen.

Nutzerdaten (Datenbank etc.) liegen unter `~/.config/uiptv` (bzw. `~/uiptv` je nach Setup)

## Bisherige Historie (chronologisch)

1. `legacy`/`legacy-dev`-Struktur auf Basis des Upstream-Legacy-Zweigs aufgebaut → GUI-Problem auf
   KDE/Wayland (linke Reiterspalte leer) → verworfen, Branches gelöscht.
2. Arch-Paketbau etabliert: `scripts/arch/PKGBUILD` + Workflow; .pkg.tar.zst wird aus dem
   jpackage-APP_IMAGE in einem `archlinux:base-devel`-Container mit makepkg gepackt.
   Wichtige Lehre: makepkg akzeptiert **keine Verzeichnisse** in source=() — App-Image wird per
   `${startdir}` eingelesen.
3. `baseline-0.1.11` erstellt (Tag v0.1.11 + Paketbau-Skripte separiert), Nutzer hat das Paket
   installiert, alte Datenbank übernommen: **Funktioniert vollständig wie die frühere debtap-Installation.**

4. **Feature „Pin 1–5“ (Version 0.1.12, in Arbeit):** 5 farbige Pin-Stufen pro Account
   (Pin 1 = rot wie alter Pin, 2 = blau, 3 = grün, 4 = orange, 5 = violett), ersetzt die einzelne
   „Pin Account on Top“-Checkbox durch 5 Checkboxen (nur eine aktiv). Account-Tab erhält eine
   Tab-Leiste „All | 1–5“ (optisch wie die Kategorie-Tabs rechts); Zahlen-Reiter filtern nach
   Pin-Farbe; Sortierung pro Reiter, Standard A–Z; alte rote Pins werden automatisch Pin 1.
   DB: neue Spalte `Account.pinLevel` (Migration 0198).

5. **Feature „Standard-Importnamen + Account Info" (Version 0.1.13):** Importe heißen künftig
   `name (0001)` (Leerzeichen, vierstellig, beide Protokolle). Umbenennung erfolgt NICHT mehr automatisch
   beim Start, sondern manuell per Terminal-Befehl `uiptv -names` (mit Fortschrittsanzeige; beendet sich
   ohne GUI-Start).
   Update 2026-11-26: Der eigene Hauptreiter „Account Info" wurde WIEDER ENTFERNT (Nutzer-Wunsch); die
   Infos stehen nur noch im Account-Tab unten im „Kontoinformationen"-Panel. Account-Daten (Status,
   Ablaufdatum, Profil-JSON) werden NUR beim Import bzw. Cache-Reload abgerufen und in der DB
   (`AccountInfo`) gecacht — NIEMALS beim bloßen Anklicken eines Accounts. Das Panel liest nur aus
   der DB; die vier Zählwerte (Live/VOD/Serien/Kategorien) sind ebenfalls in `AccountInfo` gespeichert
   (Spalten liveCount/vodCount/seriesCount/categoriesCount, Migrationen 0199–0202; `-1`/NULL = noch nie
   gezählt → Anzeige „-„) und werden NUR beim Cache-Reload bzw. Import aktualisiert (persistiert in
   `ReloadCachePopup.reloadSingleAccount`); beim Klick wird nur noch der kleine AccountInfo-Datensatz
   gelesen — nichts gezählt, keine Server-Anfrage. Für Xtream ruft der Cache-Reload jetzt zusätzlich einmal `player_api`
   (user_info) ab und speichert Ablaufdatum (exp_date) + Status + Profil-JSON — damit steht auch für
   Xtream das Expiry Date im Panel.

6. **Channel-Liste: Tooltip + horizontales Scrollen (Version 0.1.13, VOM NUTZER BESTÄTIGT):** In der
   Senderliste (ganz rechts) zeigt ein Tooltip bei Berührung den vollständigen Sendernamen (max ~420px
   breit, mit Umbruch). Die Spalte bemisst sich an der längsten gemessenen Zeile + 20 % Scrollpuffer;
   passt alles ins Fenster, füllt die Spalte die Breite aus (keine Trennlinie), sonst erscheint die
   horizontale Scrollleiste. Wichtiger Fix auf dem Weg: die Zellen löschten ihren eigenen Zellen-Text
   nicht (setText(null) fehlte) — eine zweite, überlagerte Textebene schnitt die Namen an fester Stelle
   mit „…“ ab, deshalb halfen frühere Breiten-Änderungen nicht.

7. **Kontextmenü „Pin“ in der Account-Liste (Version 0.1.13):** Rechtsklick auf markierte Accounts
   (Mehrfachauswahl möglich) → Menü „Pin“ → „Pin 1“ bis „Pin 5“ oder „Pin entfernen“: alle markierten
   Accounts erhalten die gewählte Pin-Stufe bzw. keinen Pin. Notwendig, weil bei Mehrfachauswahl im
   Account-Tab nur der zuerst angeklickte Account im Formular erscheint.

8. **Nachbesserungen (Version 0.1.13, Nutzer-Feedback-Runde 2+3):** a) Kontoinfos-Panel aktualisiert sich
   NUN WIRKLICH automatisch: `AccountInfoService.save` benachrichtigt Listener, `ManageAccountUI` horcht
   darauf (Grund für den vorherigen Fehlschlag: der Reload-Popup-Callback lief nur beim Löschen von Accounts).
   b) Backfill der Zählwerte nutzt jetzt 4 gruppierte Sammel-Abfragen (je eine für ALLE Accounts) statt 4
   Abfragen pro Account — auch auf einer 7-GB-DB in Sekunden statt Minuten; der Backfill läuft STILL und
   wird komplett übersprungen, wenn keine Account-Zahlen fehlen (kein Flackern der Account-Info-Anzeige
   mehr, keine Abfragen bei jedem Start). c) Senderliste: passt die Spalte an die Viewport-Breite an,
   solange der längste Name hineinpasst (keine Trennlinie/leerer Bereich mehr); erst wenn der längste Name
   breiter ist, wird die Spalte namenslang und die horizontale Scrollleiste kommt. Die Breitenmessung
   nutzt jetzt das aktive Theme-Stylesheet inkl. UI-Zoom, damit die längsten Namen wirklich vollständig
   hineinpassen.

## Aktuell offene Punkte / ToDos

- [x] Feature-Wunsch 1: Pin 1–5 mit Unterreitern im Account-Tab — umgesetzt in 0.1.12,
      vom Nutzer bestätigt (inkl. Fix: Pin-Reiterleiste bleibt beim Öffnen eines Accounts sichtbar)
- [x] Doku-Runde: README für den Fork neu geschrieben (Credit an Original-Autor/SPC, Hinweis auf
      die optisch andere Upstream-1.x-Linie, eigene Install-/Build-/Branch-Abschnitte);
      LICENSE um eigene Copyright-Zeile ergänzt; Upstream-Übersetzungs-Aufruf entfernt
- [x] branch protection für `baseline-0.1.11` aktiviert (2026-11-25):
      Branch per „Lock branch“ + „Do not allow bypassing the above settings“
      vollständig schreibgeschützt; dev-0.1.11 mit Standard-Einstellungen (nicht löschbar
      über UI-Regel abgedeckt, Force-Push nicht erlaubt).
      BITTE: nichts an dieser Konfiguration ändern.
- [ ] Repo ist ein öffentlicher Fork; Privatstellung nur über GitHub-Support („detach fork") möglich —
      bewusst vorerst akzeptiert (Nutzer-Entscheidung, „Option 2")
- [ ] Artifact-Aufbewahrung aktuell 1 Tag — auf Wunsch erhöhen

## Arbeitstechnik (Verbindlich!)

- Vor jedem Arbeitsschritt Repo-Zustand prüfen (`git status`, Branch, `MAINTENANCE.md` lesen)
- Kleine, prüfbare Schritte; pro Änderung ein Commit (Messages auf Englisch: was + warum)
- Backports aus dem Upstream (`xixogo5105/uiptv`, Branches `legacy`/`main`) nur **einzeln** und nur,
  wenn sie zur 0.1.11-Struktur passen; niemals ganze Zweige mergen
- Nach jedem Build: `sudo pacman -U …` Installation durch Nutzer; nur bei bestätigtem „funktioniert"
  gilt ein Schritt als abgeschlossen
- Diese Datei am Ende jeder Sitzung auf den neuesten Stand bringen
