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

9. **Reload-Fehler-Dialog neu (Version 0.1.13):** Das Popup bei globalen Abruf-Fehlern ist neu gebaut:
   „Carry On“ wurde ENTFERNT (lud bei toten Portalen stur Kategorie für Kategorie und endete trotzdem in
   Fehlern). Der Dialog ist jetzt schmal (~560 px), Text oben, darunter die Buttons untereinander in voller
   Breite, je mit seiner Checkbox darunter: „Als schlecht markieren & Weiter“, Domänen-Variante (falls
   verfügbar), ganz unten „Stop all“. Neben beiden Mark-Bad-Optionen
   gibt es je eine Checkbox „Für alle weiteren Fehler übernehmen“ — ist sie gesetzt, gelten weitere
   Fehler-Popups des gesamten Durchlaufs automatisch (sichtbar als Logzeile im Reload-Fenster), ohne
   Nachfragen. „Stop all“ ohne Checkbox. Schließen mit X/Esc = Stop all. „Mark Bad & Next“ ist der
   Standard-Button (Enter).
   ZUSÄTZLICH: Wird das Reload-Fenster per X geschlossen, läuft der Durchlauf im Hintergrund weiter; die
   Popup-Instanz samt Zustand bleibt erhalten. Der Button „Reload-Status anzeigen“ im Tab „Import Bulk Accounts“
   öffnet IMMER das letzte Reload-Fenster samt vollem Zustand (auch wenn der Durchlauf schon beendet ist);
   existiert noch keins, öffnet er ein normales neues Popup. Kontextmenü „Reload Cache“ und der Button im
   Account-Tab öffnen bei laufendem Durchlauf dasselbe Fenster, sonst ein neues mit aktueller Auswahl.

10. **Cache-/DB-Hygiene (Version 0.1.13):** a) Kontextmenü der Account-Liste: neuer Eintrag „Delete Cache“,
   wirkt auf alle markierten Accounts (mit Bestätigungsdialog und Hintergrund-Ausführung). b) BUGFIX Account-Löschung:
   `AccountService.deleteAccountData` löschte VOD/Serien-Kanäle und -Kategorien (VodChannelDb, SeriesChannelDb,
   VodCategoryDb, SeriesCategoryDb) NIE und beim Live-Category-Filter blieben Zeilen anderer accountType übrig —
   die uiptv.db wuchs daher dauerhaft weiter. Jetzt werden alle Cache-Tabellen des Accounts geleert.
   c) SQLite gibt Dateigröße nur per VACUUM frei — neue Klasse `DatabaseMaintenance` führt nach Account-Löschung
   und Cache-Löschung (debounced, im Hintergrund) ein VACUUM aus, damit die Datei real schrumpft. VACUUM-Fehler
   werden nur geloggt (best-effort, nie blockierend).

11. **Export Log, Ablauf-Indikator, README-Disclaimer (Version 0.1.13):** a) Im Reload-Popup neben „Copy Log“
   neuer Button „Export Log“: speichert denselben Text als Datei, Dateiname vorgefertigt als
   `uiptv_export_log_YYYY_MM_DD_HH_MM_SS.txt` (Zeitstempel = Moment des Speicherns). b) Account-Spalte:
   Accountnamen werden AMBER gefärbt, wenn das Ablaufdatum ≤ 7 Tage entfernt ist, ROT bei bereits abgelaufen
   (Datenbasis: gecachtes AccountInfo.expireDate, Auswertung beim Refresh); Tooltip mit dem Ablaufdatum auf dem
   Namen. KEIN Backup-Mechanismus eingebaut (Nutzer-Entscheidung: Backup manuell, weil er selbst über die
   Notwendigkeit entscheiden will). c) README um Abschnitt „Purpose and disclaimer“ ergänzt: Schulungsprojekt,
   keine Inhalte/Playlists, Nutzung auf eigene Gefahr ohne Gewähr, Einhaltung der Gesetze des jeweiligen Landes,
   keine Verantwortung für Nutzung durch Dritte.

12. **Namenskorrektur „Xtream“ (Version 0.1.13):** Der Ökosystem-Begriff ist Xtream (von stream), der
   Upstream-Autor schrieb historisch „Xtreme“. Alle NUTZER-SICHTBAREN Texte wurden auf „Xtream“ korrigiert:
   Accounttyp-Anzeige „Xtream API“, Import-Modus-Combo, Logs, Fehlermeldungen, alle 28 Übersetzungsdateien,
   README (inkl. Formulierung „It supports Xtream, Stalker Portal, M3U, and RSS …“). I interne Java-Klassennamen
   (XtremeParser, XtremeCredentialsJson, MODE_XTREME-Konstante etc.) und i18n-Schlüssel (Kleinschreibung
   „xtreme…“) bleiben unverändert — kein Risiko, kein Renewame-Aufwand. Repo-Topics konnte der Agent nicht
   setzen (403, fehlende Admin-Rechte der Integration) — Nutzer setzt sie selbst; statt „xtreme“ bitte
   „xtream-codes“ verwenden (wie bei IPTVNator).

13. **BUGFIX „Failed: Unable to execute query“ bei Xtream-Reloads (Version 0.1.13):** Ursache war das in
   Runde 10 eingebaute VACUUM: Der Trigger hing an `ConfigurationService.clearCache`, was von den Reloadern
   MITTLERWEILE im Reload aufgerufen wird — das exklusive VACUUM lief also parallel zu den Kanal-Schreibvorgängen
   und brachte sie auf einer 7-GB-DB hinter den 10-s-Busy-Timeout („Unable to execute query“). Fixes:
   a) VACUUM wird NUR noch nach abgeschlossenen Aktionen getriggert — nach Ende des kompletten Reload-Durchlaufs,
   nach expliziten Cache-Löschungen (Kontextmenü/Settings-Button) und nach Account-Löschungen — plus 8 s Ruhephase.
   b) `persistAccountInfoCounts` ist best-effort (try/catch + Log) und kann einen Reload nie mehr fehlschlagen lassen.
   c) Reload-Fehlertexte zeigen jetzt die SQLite-Ursache statt der generischen Wrapper-Meldung
   („Failed: [SQLITE_BUSY] database is locked“ o. ä.), damit künftige Fehler sofort diagnostizierbar sind.

14. **Import-Namensschema, Duplikat-Schutz, Clean Accounts (Version 0.1.13):** a) Der M3U-Import vergab
   Legacy-Namen `host (1)`, `host (10)` — `UiptUtils.getUniqueNameFromUrl` übershen in der Umbenenn-Runde;
   jetzt 4-stellig (`host (0001)`). b) „Group Accounts“ ist default AB (manuell zuschaltbar); gruppierte
   Accounts behalten bewusst den nackten Server-Namen ohne Klammer (kennzeichnet gruppierte Einträge).
   c) Individual-Importe prüfen Vorhandensein: Stalker = Server+MAC, Xtream = Server+Username+Passwort,
   M3U = identischer Pfad; existierende werden übersprungen und nach dem Import als Liste gemeldet
   (Alert mit Namen; Annahme nicht in den Reload-Windows-Status — das kommt mit dem geplanten
   Gruppierungs-Rework). d) NEU „Clean Accounts“ (Settings → „Account Maintenance“): Popup normalisiert
   Kontennamen (wie `uiptv -names`) und löscht echte Dupletten (gleiche Kriterien wie c; NICHT nach
   Namen), inkl. Fortschrittsbalken, Restzeitschätzung, Log, Copy/Export Log, „Stop nach aktuellem
   Schritt“; am Ende VACUUM (debounced). Behalten wird jeweils der zuerst angelegte Account.

15. **BUGFIX Namens-Normalisierung (Version 0.1.13, Nutzer-Feedback):** Warum `uiptv -names` und Clean Accounts
   „renamed: 0“ meldeten, obwohl `(1)`-Einträge existierten: Der Algorithmus behielt die Originalnummer suffixierter
   Konten; kollidierte das formatierte Ziel (z. B. `(0001)` existiert bereits), wurde das Umbenennen STILL
   übersprungen — `(1)` konnte so nie repariert werden. Neu: kollidierende Legacy-Suffixe erhalten die nächste freie
   Nummer; nackte (gruppierte) Konten werden NICHT mehr nummeriert (behalten bewusst den nackten Namen); Matching
   toleriert Leerzeichen am Rand/im Namen. `de.test.eu (0030) -> de.test.eu (0029)` im Clean-Popup war KEIN Rename,
   sondern die Dupletten-Löschung (gelöscht: (0030), behalten: (0029)) — Dupletten-Erkennung funktioniert also.

16. **Duplikat-Werkzeuge (Version 0.1.13):** a) Terminal: `uiptv -duplicates` entfernt exakte Dupletten
   (Stalker Server+MAC inkl. MAC-Liste, Xtream Server+Zugangsdaten inkl. Credentials-JSON, M3U identischer
   Pfad) mit Fortschrittsanzeige; `uiptv -names` bleibt unverändert. b) Settings → „Account Maintenance“ hat
   jetzt ZWEI Buttons: „Clean Accounts“ (Namen normalisieren + Dupletten, wie bisher) und „Clear Duplicates“
   (nur Dupletten); beide nutzen dasselbe Popup (Fortschritt, Restzeit, Copy/Export Log, Stop nach aktuellem
   Schritt, abschließendes debounced VACUUM). c) Import-Duplikat-Schutz vergleicht jetzt auch MAC-Listen und
   Xtream-Credentials-JSON (nicht nur primäre Werte).

17. **BUGFIX SQLITE_BUSY beim Duplikate-Entfernen + Startverzögerung (Version 0.1.13):** Wie beim Reload-Problem
   hing ein VACUUM-Trigger IN `deleteAccountData` — jedes gelöschte Duplikat startete das exklusive VACUUM,
   die Folge-Löschungen liefen in den Lock („[SQLITE_BUSY] database is locked“), der Lauf starb (Terminal:
   Stacktrace/Exit; Popup: Fortschritt/Restzeit stand still, weil der Hintergrundthread tot war — kein
   Fehler im UI). Fixes: a) VACUUM nur noch am ENDE von Lösch-Batches (Kontextmenü-Löschloop, Delete-Account-
   Task, deleteAll, Cleanups — Letzteres war schon korrekt). b) Cleanup verzeiht Einzel-Fehler („FAILED: …“-Logzeile)
   und läuft weiter. c) Das gelegentliche Aufhängen nach dem ERSTEN Duplikat war genau dieser Lock-Tod.
   Die 10–15 s Startverzögerung war sehr wahrscheinlich das vom abgestürzten Lauf hinterlassene große WAL-File,
   das beim App-Start zurückgespielt wurde — nach einem sauberen Cleanup-Durchlauf prüfen.

18. **Expiry-Pin (Version 0.1.13):** Automatischer „Uhr-Pin“ (Level 6, nur automatisch vergebbar — die Pin-
   Checkboxen im Account-Formular bleiben 1–5) für Konten, die binnen 7 Tagen ablaufen oder bereits abgelaufen
   sind. Account-Spalte: sechster Reiter mit Uhr-Symbol (⏰) filtert Level 6; in der Zeile erscheint statt des
   Pin-Symbols eine rote Uhr. Auslöser: a) Terminal `uiptv -expiry` (Fortschritt + „expiry pin changed: …“),
   b) Settings → Account Maintenance → „Pin Expiring Accounts“ (dritter Button, geteiltes Popup mit
   Fortschritt/Restzeit/Copy/Export Log). Lauf: liest nur die winzige AccountInfo-Tabelle (1 Zeile/Konto) —
   auch bei 7-GB-DB in Sekundenbruchteilen bis wenigen Sekunden fertig; die Kanal-Tabellen werden NICHT
   berührt. Kollisions-Schutz: manuelle Pins 1–5 werden nie geändert; Speichern eines Uhr-Pin-Kontos im
   Account-Formular behält Level 6, wenn kein manueller Pin gewählt wurde; „Unpin“ im Kontextmenü setzt 0
   (nächster Scan pinnt ggf. wieder); erneutes Pinnen/Entpinnen läuft über den Scan.
   d) Der Scan läuft zusätzlich automatisch EINMAL beim App-Start (Daemon-Thread nach Tab-Initialisierung,
   still im Hintergrund, best-effort — Ausführung wie der manuelle Scan, nur ohne UI-Popup).
   e) Die Terminal-Befehle `-names`/`-duplicates`/`-expiry` sind Strg+C-sicher: ein Shutdown-Hook
   (TerminalInterruptGuard) wartet auf das Ende des aktuellen Kontos (max. 30 s, Logzeile
   „interrupted, exiting after the current account“) und beendet dann sauber; SQLite-Transaktionen
   garantieren, dass die DB auch bei Abbruch konsistent bleibt.

19. **Kontextmenü-Verify (Version 0.1.13):** „Verify“-Untermenü in der Account-Spalte (Rechtsklick) für
   Stalker- und Xtream-Konten, wirksam auf alle markierten Konten. Untermenü-Einträge = Verzögerungswahl
   (1 s – 30 min, Standard 10 s, wie im Settings-Verify). Ablauf pro Konto: alle MACs (Stalker) bzw. alle
   Username/Passwort-Kombinationen (Xtream, auch Gruppen-JSON) werden LIVE geprüft — Stalker wie bisher
   (Handshake + Kategorieabruf), Xtream NEU über player_api user_info (echter Login-Test, NICHT das
   gecachte Ablaufdatum; auth=0 bzw. HTTP 401/403 = INVALID). Anzeige im ProgressDialog wie Settings-Verify
   (Titel „Verifying Accounts...“), mit Kopfzeile pro Konto und Expiry/Status-Infozeilen. NEU: getrenntes
   „Server Status“-Fenster rechts im Dialog (erscheint nur bei Fehlern) mit Meldung + Kontoname +
   MAC/user:pass — Server nicht erreichbar/HTTP-Fehler wird als [ERROR] geloggt (Balkensegment amber statt
   rot/grün), damit „kurzzeitig down“ von „abgelaufen“ unterscheidbar ist. SERVER-MERKEN pro Lauf: Ein
   serverbezogener Fehler (unreichbar, Connection Reset, Timeout, HTTP 404, kaputte JSON-Antwort) markiert
   den Server für den Rest des Laufs — weitere MACs/Kombos desselben Servers werden sofort als „Skipped“
   geloggt (amber, ohne erneutes Warten); NICHT gemerkt werden Invalid/HTTP 401/403 (kontospezifisch).
   Verify-Timeout 10 s (statt 30 s Standard). Jede FEHLGESCHLAGENE Prüfung
   (INVALID oder Server-Fehler) erscheint rechts als Zeile mit CHECKBOX; „Delete Selected“-Button unten
   rechts löscht die angehakten KONTEN nach Bestätigung, danach VACUUM (identisch für Stalker und Xtream;
   der frühere automatische Lösch-Dialog am Ende wurde durch die Checkbox-Auswahl ersetzt — kein Ausschluss
   mehr bei gemischten Fehlern, Entscheidung liegt beim Nutzer). Dialog: 900×620, NICHT-modal (App bleibt
   während langsamer Server-Tests nutzbar), Copy/Export-Buttons pro Bereich (Dateinamen uiptv_verify_log_*
   bzw. uiptv_verify_status_* mit Zeitstempel), Verify-Anfragen mit 10 s Timeout; Fehler-Kategorien-Gruppierung
   (errorCategory: Connect timed out / Connection refused / Reset / Network unreachable / DNS resolution failed /
   HTTP xxx / JSON), Kopf-Checkbox je Kategorie mit Tooltip-Empfehlung (Delete/Retry/Wait/VPN), Auto-Scroll nur
   wenn unten gepinnt, Select All + Delete Selected (Löschen nach Bestätigung + VACUUM).
   Kern: `AccountVerificationService` (CredentialCheck-Record mit valid/serverError/errorMessage),
   `XtremeParser.fetchUserInfo` (mit RequestOptions), ProgressDialog.addProblemEntry/addErrorResult.

20. **VPN-Tag beim Bulk-Import (Version 0.1.14):** In Settings → Import Bulk Accounts unter „Start verification
   after parsing“ eine „VPN“-Checkbox + 3-Zeichen-Textfeld (Länderkürzel, z. B. US/USA). Angewählt erhält jeder
   importierte Account den Zusatz „ (VPN)“ bzw. „ (VPN: US)“ HINTER der vierstelligen Nummer — z. B.
   „anonym.com (0012) (VPN: US)“; alphabetische Sortierbarkeit bleibt erhalten. Nummernvergabe läuft im
   eigenen Namensraum (Nummern werden mit Suffix geprüft, Kollisionen mit ungetaggten Konten unmöglich);
   gilt für Stalker, Xtream und M3U sowie für gruppierte Imports (nackter Servername + Suffix). Duplikat-
   Erkennung unverändert (Server+MAC bzw. Server+Credentials bzw. Pfad). Suffix wird beim Clear zurückgesetzt.
   `uiptv -names` fasst getaggte Namen nicht an (matchSuffix erkennt (NNNN) am Ende nicht → wird als „nackt“
   übersprungen). Kern: AccountParser.setAccountNameSuffix, TextParserService.saveBulkAccounts-Überladung,
   ParseMultipleAccountUI.resolveVpnNameSuffix, UiptUtils.getUniqueNameFromUrl(url, suffix).

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
