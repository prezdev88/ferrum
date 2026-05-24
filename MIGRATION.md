# Migration (GTK → Electron)

This document tracks the migration of Ferrum’s desktop frontend from `front/` (Python/GTK4/libadwaita) to an Electron-based UI, while keeping the Spring Boot backend (`back/`) as the API/scraper layer.

## Scope

- Target platforms: Windows, macOS, Linux
- Keep backend API contract stable where possible (`/api/*`)
- Migrate features one-by-one with shippable milestones

## Feature checklist

### App lifecycle

- [x] App boots and shows a startup screen/state
- [x] Backend process management (start/stop with the app)
- [x] Backend health check and “backend not ready” UX
- [ ] Backend URL/port configuration (env or app config)

### Search

- [x] Search form with query + search type selector
- [x] Supported search types:
  - [x] `BAND_NAME`
  - [x] `MUSIC_GENRE`
  - [x] `THEMES`
  - [x] `ALBUM_TITLE`
  - [x] `SONG_TITLE`
  - [x] `LABEL`
  - [x] `ARTIST`
  - [x] `USER_PROFILE`
  - [x] `GOOGLE`
- [x] Search results list (bands)
- [x] Results count / empty state / error state

### Band details

- [x] Band detail view (name, image, metadata)
- [x] Discography list
- [x] Discography filtering by release type
- [x] “Open in Metal Archives” action

### Album details

- [x] Album modal/dialog
- [x] Tracklist rendering
- [x] “Open in Metal Archives” action
- [x] “Search on provider” action per track/album

### Provider actions

- [x] Provider selection:
  - [x] YouTube Music
  - [x] YouTube
- [x] Open external URLs in system browser

### Favorites

- [x] Star/unstar band
- [x] Favorites list view
- [x] Favorites persistence (local storage)

### Search history

- [x] Load search history from backend (`/api/search-history`)
- [x] Apply history entry back into the search flow

### Personalization / Settings

- [x] Theme modes:
  - [x] System
  - [x] Light
  - [x] Dark
  - [x] Black
- [x] Album type color mapping editor
- [x] Settings persistence (local storage)

### Error handling & UX polish

- [ ] Network/API error normalization and user-facing messages
- [x] Loading states for long requests
- [x] Remote image loading for band/album artwork with fallbacks

### Packaging & distribution

- [ ] Development mode: run UI + backend locally with hot reload
- [ ] Production bundle includes backend + runtime (no separate Java install)
- [ ] Playwright browser/runtime strategy (bundled vs first-run download)
- [ ] Installers per platform (Win/macOS/Linux)
- [ ] App versioning aligned with repo tags/releases
