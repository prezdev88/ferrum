# Ferrum 🤘

**Ferrum** is a desktop app to explore **Metal Archives** with a clean split between:
- 🖥️ Electron frontend
- ⚙️ Spring Boot backend API

Built for fast discovery: search bands, inspect discographies, open album modals, and personalize your flow with favorites, themes, and provider-aware actions.

## ✨ Highlights

| Area | What you get |
|---|---|
| 🔎 Search | Multi-mode search (`BAND_NAME`, `MUSIC_GENRE`, `THEMES`, `ALBUM_TITLE`, and more) |
| 🧾 Band view | Metadata cards + full discography list |
| 🗂️ Discography UX | Filter by release type (`All types`, `Demo`, `Full-length`, etc.) |
| 💿 Album modal | Tracklist + Metal Archives link + search in selected provider |
| ⭐ Favorites | Star/unstar bands and browse favorites in the same Results panel |
| 🎨 Personalization | `System`, `Light`, `Dark`, `Black` themes |
| 🏷️ Visual tuning | Editable color mapping by album type in Settings |
| 🕘 Search history | Loaded from backend cache and reusable from UI |

## 🧱 Architecture

```text
Electron Frontend (front-electron/)
  -> consumes HTTP API
Spring Boot Backend (back/)
  -> exposes HTTP API
  -> Metal Archives scraping + cache
```

## 🚀 Quick Start

From repository root:

```bash
./run-electron.sh
```

Startup flow:
1. Checks for a backend JAR in `back/target`.
2. If missing, builds backend with Maven.
3. Backend starts at `http://localhost:18080` (default).
4. Electron app launches and consumes the local backend.

## 📁 Repository Layout

- `front-electron/`: Electron app (UI, themes, settings, favorites UX)
- `back/`: Spring Boot backend + endpoints
- `run-electron.sh`: Launcher script (builds & starts backend, launches Electron)

## 💾 Local User Data

Ferrum stores local data under `~/.config/ferrum/`:

- `preferences.json`: theme mode, music provider, album-type colors
- `favorites.json`: favorite bands
- `session.json`: backend/session runtime data (when present)

## 🔌 API Reference

All endpoints and `curl` examples:

- [ENDPOINTS.md](/home/prezdev/git-projects/ferrum/back/ENDPOINTS.md)

## 📦 Arch Linux (AUR)

Install from AUR:

```bash
yay -S ferrum-git
```

AUR package page:

- https://aur.archlinux.org/packages/ferrum-git
