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

UIPTV is a universal IPTV player written in Java for Windows, Linux, and macOS.
It plays Live TV, VOD, and Series from Stalker portals, M3U playlists,
Xtreme Codes, and RSS feeds — with an embedded player, external players, or
a local-network web app.

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
- **Other systems:** build installers with Maven (see below) or use the
  portable app bundle (jpackage app-image) that the standard upstream build
  produces for Windows, Linux, and macOS.
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

## License

MIT — see [LICENSE](LICENSE). This project is not affiliated with or endorsed
by the upstream author; it is a private test project.
