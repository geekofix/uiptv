# UIPTV (Legacy Line) — geekofix fork

> [!NOTE]
> **Fork notice — please read:** This repository is a personal test project and a
> fork of [xixogo5105/uiptv](https://github.com/xixogo5105/uiptv). **All credit for
> the original application goes to the upstream author (SPC)**, who continues to
> develop the project on GitHub with a completely redesigned user interface
> (versions 1.0 and newer).
>
> **This fork is not that version.** It is based on the classic interface of the
> original **Release 11 (v0.1.11)** and continues that legacy line with own fixes
> and features — intentionally close to the original look and feel, not the 1.x
> redesign. If you come from the upstream project and something looks different
> here, that is why.
>
> Development is supported by [Mistral AI](https://mistral.ai) (Vibe Code).
> This project is not affiliated with or endorsed by the original author.
> License stays MIT, see [LICENSE](LICENSE).

UIPTV is a universal IPTV player written in Java. **This fork is a
Linux-only project:** it is built, maintained, and tested on Linux only
(Arch Linux / CachyOS). Upstream code contains Windows and macOS support,
but this fork does not build, verify, or support those platforms — a clean
Windows or macOS port would require maintainers who can actually test on
them.
It supports Xtream, Stalker Portal, M3U, and RSS — with an embedded player,
external players, or a local-network web app.

This fork keeps the classic, low-friction desktop UI and adds its own improvements,
for example five colored pin levels for accounts with matching sub-tabs
(introduced in 0.1.12).

## Installation

Prebuilt packages are built automatically by GitHub Actions on every change
("Baseline 0.1.11 Arch package" workflow, artifact `UIPTV-pacman`):

- **Arch Linux / CachyOS:** download the artifact, unzip, then
  `sudo pacman -U uiptv-<version>-x86_64.pkg.tar.zst`.
  Update with the same command using a newer package file; older package files
  can be kept to roll back at any time.
- **Rename imported accounts to the current scheme** (e.g. after restoring an
  old database) with the terminal command `uiptv -names`. It shows live
  progress and exits without starting the app.
- **Remove duplicate accounts** (same server+MAC or same server+credentials)
  with the terminal command `uiptv -duplicates`. It shows live progress and
  exits without starting the app; the same cleanup also runs from the
  "Account Maintenance" section in the settings.
- **Other systems:** not provided by this fork. It is Linux-only — see the
  note above. Use the upstream project if you need Windows or macOS builds
  or try building from source at your own risk.
- VLC is recommended for the best embedded playback experience.

## Building from source

```bash
./mvnw -B clean package -DskipTests
```

Native installers (`.deb`, `.rpm`, app-image) are produced through the jpackage
profiles of the upstream build, e.g.:

```bash
./mvnw -B clean package -DskipTests -P linux-x86_64,pkg-app-image
```

Java 25 or later is required.

## Project layout

- `src/` — single-module application code (legacy 0.1.11 structure, kept intentionally)
- `web/` — local web app assets
- `website/` — project website sources
- `scripts/arch/` — PKGBUILD recipe used to create the Arch/pacman package
- `MAINTENANCE.md` — project status, branch strategy, and work rules for this fork
- `GUIDE.md` — upstream user guide (still largely applicable)
- `ACCOUNT_IMPORT_GUIDES.md` — account import how-tos

## Branches

| Branch | Meaning |
|---|---|
| `baseline-0.1.11` | Immutable base (branch-protected): exact upstream tag `v0.1.11` plus packaging scripts |
| `dev-0.1.11` | Default branch; working branch for all fixes and features |
| `main` | Untouched upstream reference (1.1.1 redesign) |

## Versioning

Versions count up from the upstream base (`0.1.11` → `0.1.12` → …) whenever a
feature milestone is completed and confirmed working.

## Purpose and disclaimer

This software is a private, non-commercial **training and educational project**.
Its only purpose is to study and practice Java, JavaFX, database handling, and
media playback techniques.

**UIPTV is a player: it does not provide or sell subscriptions, channels,
playlists, or other media.**

- **No content:** the software ships **without any playlists, channels, streams,
  or credentials**. It contains no providers and grants access to nothing.
  Everything the user views must come from accounts and data sources they own
  or are authorized to use.
- **No warranty, use at your own risk:** the software may contain bugs. It is
  provided "as is", without any warranty of any kind. The user is responsible
  for possible data loss or other consequences of using it — create backups of
  your data (especially `uiptv.db`) before large operations.
- **Local laws apply:** users must comply with the laws of their respective
  country when using this software. The authors of this project do not condone
  or support any illegal use of it and accept no liability for how the software
  is used by third parties.

## License

MIT — see [LICENSE](LICENSE). This project is not affiliated with or endorsed
by the upstream author; it is a private test project.
