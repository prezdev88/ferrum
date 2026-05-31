# Changelog

All notable changes to this project will be documented in this file. See [standard-version](https://github.com/conventional-changelog/standard-version) for commit guidelines.

## [1.4.0](https://github.com/prezdev88/ferrum/compare/v1.3.0...v1.4.0) (2026-05-31)


### Features

* add album grid size adjustment feature with settings control ([2a0b88f](https://github.com/prezdev88/ferrum/commit/2a0b88fdac00f8791f70fa68a9109c53d421ffae))
* add option to show album info in band view and update related settings ([0e42a11](https://github.com/prezdev88/ferrum/commit/0e42a11b281c5b16d62172b5ec34bd6218ed9218))
* adjust album cover prefetch bar styles for improved appearance ([1f71364](https://github.com/prezdev88/ferrum/commit/1f7136426b8016169528f55053d68002463d3da7))
* adjust padding and max-height in discography and album rows for improved layout ([e7aee44](https://github.com/prezdev88/ferrum/commit/e7aee4400c6691c2750d005963d1636f03c47fcf))
* adjust padding in modal for improved content visibility ([ca63209](https://github.com/prezdev88/ferrum/commit/ca63209a9f64a10bec54b79259e1c0235efff5a6))
* album to modal ([39be56b](https://github.com/prezdev88/ferrum/commit/39be56b4d84dff945597757a72e6e6d37160b836))
* implement album cover prefetching with progress display ([0010f74](https://github.com/prezdev88/ferrum/commit/0010f7447fe9ee87e76634cc2cae1989da55a575))
* refactor discography layout and improve album modal handling ([6e9ab0d](https://github.com/prezdev88/ferrum/commit/6e9ab0dfa920c014884b754cfd1daec31dddabc0))
* refactor jewelcase spine width calculations for improved responsiveness ([a6a38eb](https://github.com/prezdev88/ferrum/commit/a6a38eb50fd588358e1dcce328c0e0ab421b136d))
* simplify album modal by removing header elements and add escape key functionality for closing modals ([10be74a](https://github.com/prezdev88/ferrum/commit/10be74af71e42f8c2690b4371e8b62c8564e0da2))
* update album cover image style to use 'contain' for better fit ([4ac195b](https://github.com/prezdev88/ferrum/commit/4ac195b581420bbbfbbcd1711f10e8ea8754abb0))
* update discography shelf and album row styles for improved UI consistency ([3cbd4d3](https://github.com/prezdev88/ferrum/commit/3cbd4d3b73ac2f34ac83b8cf7b510dbab3e7ff37))

## [1.3.0](https://github.com/prezdev88/ferrum/compare/v1.2.0...v1.3.0) (2026-05-30)


### Features

* adjust padding and height for band hero card and artwork; update discography filter logic ([4250bf1](https://github.com/prezdev88/ferrum/commit/4250bf1fd467a651162e541113abe5a35fec4d0c))
* enhance album inspector and discography layout with responsive design ([3258e0d](https://github.com/prezdev88/ferrum/commit/3258e0d12e0f430d529a6aea9a16ee8bec58562b))

## [1.2.0](https://github.com/prezdev88/ferrum/compare/v1.1.0...v1.2.0) (2026-05-30)


### Features

* add caching functionality for band and album details ([f8154a4](https://github.com/prezdev88/ferrum/commit/f8154a4b926444326fba979fd011010061a3323b))
* enhance album display with jewelcase and compact disc visuals ([b379921](https://github.com/prezdev88/ferrum/commit/b3799211a37e7e07283f0eccdb53c593a519dfe8))
* implement now playing bar and embedded player functionality ([9fd278c](https://github.com/prezdev88/ferrum/commit/9fd278c4e6596f77ea077becfef0bb6b0e5b89b7))


### Refactor

* remove backend, models, settings, run script, and styles from the Ferrum Gnome project ([62bfa0d](https://github.com/prezdev88/ferrum/commit/62bfa0db7f49a5cf4387f51a2eab9143fe75cc68))
* remove unused AlbumDetail cache handling and localization files ([1643deb](https://github.com/prezdev88/ferrum/commit/1643debf83e9f9c4c4ef7d9714d6aef10a0d90c0))
* update project structure to use Electron frontend and remove GTK components ([c93269e](https://github.com/prezdev88/ferrum/commit/c93269ee1b8b3709613f9250aa27732fb2a1d830))

## [1.1.0](https://github.com/prezdev88/ferrum/compare/v1.0.0...v1.1.0) (2026-05-24)


### Features

* add country flag functionality based on ISO codes in band results ([546ed75](https://github.com/prezdev88/ferrum/commit/546ed75e20a5bbafed6dc99d13fbd9d6f52999b9))
* add favorite logo opacity and image-only settings for favorites ([8d07d8b](https://github.com/prezdev88/ferrum/commit/8d07d8bb5196757e1b8db26a21ec20a7360031c8))
* add refresh functionality for selected band and clear band cache API ([6fab621](https://github.com/prezdev88/ferrum/commit/6fab621b23df3ec91762bae4ef176005e00fffe0))
* add support for explicit backend JAR path configuration ([f843552](https://github.com/prezdev88/ferrum/commit/f8435524f91dac0cb74726112096ae3bdc377db4))
* **electron:** initialize front-electron app with preload and renderer scripts ([016ed4f](https://github.com/prezdev88/ferrum/commit/016ed4f09c4aa0443e0f39f0dc5f87b23a8b2479))
* enhance band hero card layout and add status chip functionality ([e22f5e2](https://github.com/prezdev88/ferrum/commit/e22f5e2d534b9d9cb28f17b1a663b921041dacfc))
* enhance band hero card styling and add z-index for modal ([24dbd73](https://github.com/prezdev88/ferrum/commit/24dbd73813cc6fa5be99e331bcacc8d1f59c8923))
* enhance band hero card styling and adjust album list alignment ([3acf3f5](https://github.com/prezdev88/ferrum/commit/3acf3f5292a508fc6708e9a31a3a8894341ec036))
* enhance band hero card styling and update markup in renderer ([20ffacf](https://github.com/prezdev88/ferrum/commit/20ffacfe0fa9a214b4e5ea24ef865813ecd999c3))
* implement ClearBandCacheUseCase and add API endpoint for clearing band cache ([dc5c400](https://github.com/prezdev88/ferrum/commit/dc5c400a4701b045668357634864e1da930a95ed))
* update discography section layout and enhance styling for better visibility ([c74b7bc](https://github.com/prezdev88/ferrum/commit/c74b7bc919480d446d4baf561c4434ec42d3cec0))

## 1.0.0 (2026-05-24)


### Features

* add .SRCINFO and update PKGBUILD and README.md for AUR package support ([79621b0](https://github.com/prezdev88/ferrum/commit/79621b01d4e3b32f06a3ad519032646b9c2cc402))
* add API documentation for backend endpoints including health check, search, and details retrieval ([a73587a](https://github.com/prezdev88/ferrum/commit/a73587a2832e01749c661f408e562428a5135982))
* add black theme mode; update theme preferences and styles for black mode support ([585a86c](https://github.com/prezdev88/ferrum/commit/585a86c9a8e98fbb1df3457a94c7e6cda68a11e6))
* add cache to app ([91541a1](https://github.com/prezdev88/ferrum/commit/91541a1fe02cc30d5a35028a416cea082b41f141))
* add discography type filtering to FerrumWindow; implement dropdown and populate logic for album display ([8d10704](https://github.com/prezdev88/ferrum/commit/8d1070498ffeeb73839bce41c77a12b7dec79de5))
* add exit confirmation dialog with localization support ([7559e5c](https://github.com/prezdev88/ferrum/commit/7559e5c06375231e05a3ec415c3718e60faa1c41))
* add imageUrl field to AlbumDetail and BandDetail; update scraper and UI to support artwork display ([57d1e57](https://github.com/prezdev88/ferrum/commit/57d1e5753e8e7d572327f349b5dc921fcb3da4f7))
* add imageUrl to AlbumEntry; enrich discography with cached images in backend; update scraper and UI for artwork display ([13c1712](https://github.com/prezdev88/ferrum/commit/13c17126dff30cb0565d79017fa815a071b069d9))
* add multilingual support for UI with language configuration and localization ([f6b5274](https://github.com/prezdev88/ferrum/commit/f6b52743d9016458327b168d4b7300362ea26454))
* add music search functionality and configuration options for tracks in Lanterna UI ([15d5aa8](https://github.com/prezdev88/ferrum/commit/15d5aa8fa8ad7cd30f1c5e38a24024e1a54858a4))
* add PKGBUILD and README.md for initial project setup and documentation ([903d962](https://github.com/prezdev88/ferrum/commit/903d962d16c84516998c7026bdd0b9efd0412fb5))
* add search button for albums in provider; implement functionality to open album search in YouTube or YouTube Music ([0937142](https://github.com/prezdev88/ferrum/commit/093714246517710c9f0f8be2d70dee3fe272aba9))
* add SettingsStore and UserSettings for theme and music provider preferences ([5ec3732](https://github.com/prezdev88/ferrum/commit/5ec3732fe1dd19673e6273cf044db8310e35725d))
* add user interface implementations and configuration for Metallum ([ce9b778](https://github.com/prezdev88/ferrum/commit/ce9b778e2b0ec6321b7c697cfad1a84f7c9312e1))
* adjust margins for detail content in FerrumWindow for improved layout ([a507ea0](https://github.com/prezdev88/ferrum/commit/a507ea06108ae784897031dffa61284cdad961b6))
* enhance console and lanterna UI for better album and band detail display ([47fba91](https://github.com/prezdev88/ferrum/commit/47fba913e22a0c7f390c405f613e806ef7f392d1))
* enhance settings dialog styling with new CSS classes for better layout ([39f40c5](https://github.com/prezdev88/ferrum/commit/39f40c598d19c84ee3394e172d4401b106e91af9))
* implement album type color management; add dynamic CSS rules and UI for album type settings ([ab07d58](https://github.com/prezdev88/ferrum/commit/ab07d5857e847a284a880815a68d0a2d5e7da553))
* implement band search functionality with multiple search types ([28ef6b8](https://github.com/prezdev88/ferrum/commit/28ef6b8bd4beca98cfbcf62aeb69dd15ef3ae68d))
* implement discography type filter persistence by band; add logic to resolve and save selected filter ([7314811](https://github.com/prezdev88/ferrum/commit/7314811f46a177b1db24e3fc444c3bb93095588a))
* implement favorites functionality; add favorite bands management and UI updates ([db220f4](https://github.com/prezdev88/ferrum/commit/db220f4d0f593eb7b47980a76fc44f660113bed4))
* Implement Lanterna UI for Ferrum application ([94bfae6](https://github.com/prezdev88/ferrum/commit/94bfae6667e876bd71654ab2203fcd6b4e1ebb63))
* implement search history functionality; add SearchHistoryEntry model, use case, and API integration ([09c3ef2](https://github.com/prezdev88/ferrum/commit/09c3ef2d18e22aef030546e068b3b87dcb128657))
* implement theme preferences; add support for light and dark modes in UI ([eb3ae51](https://github.com/prezdev88/ferrum/commit/eb3ae515fa0c130a1fdf8a847eb38dfc93f912d2))
* improve album detail display in Lanterna UI and refactor track title extraction ([8c8c7fa](https://github.com/prezdev88/ferrum/commit/8c8c7fa59afc02677d21e1f3774498af3d988ded))
* improve focus handling for albums table when band details are loaded ([27012d4](https://github.com/prezdev88/ferrum/commit/27012d40abab36ab46703a24988ff5000236cabe))
* init ([c9d605a](https://github.com/prezdev88/ferrum/commit/c9d605a810c31576b3949fdbb3660b0a888e66b4))
* refactor AlbumWindow to AlbumDialog; add LoadingDialog for album loading state ([a81feec](https://github.com/prezdev88/ferrum/commit/a81feecede4b50cfcacc39e02dc2acee42c265dc))
* refactor loading dialog handling in FerrumWindow; remove album loading dialog and unify loading dialog methods ([bb2128a](https://github.com/prezdev88/ferrum/commit/bb2128a0b3caad0cb94fd3ce6aac93cfe006e169))
* separate in back and front ([2826d86](https://github.com/prezdev88/ferrum/commit/2826d8624ca4ae70a5d35c358354d5d926c492ba))
* update results table headers and improve focus handling in search results ([5772fcf](https://github.com/prezdev88/ferrum/commit/5772fcfd830340bc15ecbad35262e2546d01e2bb))


### Refactor

* change back and front name folder ([c1b8d9d](https://github.com/prezdev88/ferrum/commit/c1b8d9d0580c499f468c235001289cf799c0ff7f))
* remove tui interface. now clean architecture is the main rule in backend ([6eecfc8](https://github.com/prezdev88/ferrum/commit/6eecfc8ceee9813ef1bc1b6724a73731a99a6dd3))
* rename app to ferrum ([283fd5f](https://github.com/prezdev88/ferrum/commit/283fd5fdd23a32c173a9afed78b0482a710dc315))
* simplify loop for processing album rows in MetallumScraperGateway ([db2ac1b](https://github.com/prezdev88/ferrum/commit/db2ac1b68d789608c349153a312efa82f930edbd))
* update artwork handling; remove min-width and min-height styles for artwork classes ([b463a4f](https://github.com/prezdev88/ferrum/commit/b463a4fd50aa9040e3ed55280346af82c2eb714f))
