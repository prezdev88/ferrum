from __future__ import annotations

import atexit
import json
import re
import subprocess
from datetime import timedelta
from pathlib import Path
from threading import RLock
from time import monotonic
from urllib.parse import quote, urljoin

from bs4 import BeautifulSoup
from playwright.sync_api import Browser, BrowserContext, Page, Playwright, TimeoutError, sync_playwright

from .cache import FileCache
from .models import AlbumDetail, AlbumEntry, BandDetail, BandSummary, TrackEntry

BASE_URL = "https://www.metal-archives.com/"
SEARCH_URL = BASE_URL + "search?searchString={query}&type={search_type}"
SESSION_FILE = Path.home() / ".config" / "ferrum" / "session.json"
SPRING_DIR = Path(__file__).resolve().parents[2] / "back"

SEARCH_TYPE_REQUESTS = {
    "BAND_NAME": "band_name",
    "MUSIC_GENRE": "band_genre",
    "THEMES": "band_themes",
    "ALBUM_TITLE": "album_title",
    "SONG_TITLE": "song_title",
    "LABEL": "label_name",
    "ARTIST": "artist_name",
    "USER_PROFILE": "user_profile",
    "GOOGLE": "google",
}

SEARCH_TTL = timedelta(days=1)
BAND_TTL = timedelta(days=7)
ALBUM_TTL = timedelta(days=30)


class ScraperError(RuntimeError):
    pass


class CloudflareSessionError(ScraperError):
    pass


class FerrumScraper:
    def __init__(self, cache: FileCache | None = None, session_file: Path | None = None) -> None:
        self.cache = cache or FileCache()
        self.session_file = session_file or SESSION_FILE
        self.visible = False
        self._lock = RLock()
        self._java_session_refresh_attempted = False

        self.playwright: Playwright | None = None
        self.browser: Browser | None = None
        self.context: BrowserContext | None = None
        self.page: Page | None = None

        self._start_browser()
        atexit.register(self.close)

    def close(self) -> None:
        with self._lock:
            if self.context is not None:
                self.context.close()
                self.context = None
            if self.browser is not None:
                self.browser.close()
                self.browser = None
            if self.playwright is not None:
                self.playwright.stop()
                self.playwright = None
            self.page = None

    def search(self, query: str, search_type: str) -> list[BandSummary]:
        normalized_query = self._normalize_query(query)
        normalized_type = search_type.strip().upper()
        if normalized_type not in SEARCH_TYPE_REQUESTS:
            raise ScraperError(f"Unsupported search type: {search_type}")

        cache_key = f"{normalized_type.lower()}::{normalized_query}"
        cached = self.cache.read("search", cache_key, self._load_band_summaries)
        if cached is not None:
            return cached

        request_type = SEARCH_TYPE_REQUESTS[normalized_type]
        search_page_url = SEARCH_URL.format(query=quote(query.strip()), search_type=request_type)

        with self._lock:
            page = self._require_page()
            try:
                with page.expect_response(
                    lambda response: "ajax" in response.url and response.status == 200,
                    timeout=10_000,
                ) as response_info:
                    page.goto(search_page_url, wait_until="domcontentloaded")
                results = self._parse_search_results(json.loads(response_info.value.text()), normalized_type)
            except TimeoutError:
                page.goto(search_page_url, wait_until="domcontentloaded")
                self._ensure_not_challenge_page(page)
                results = self._parse_search_results_from_html(page.content(), search_page_url, normalized_type)

        self.cache.write("search", cache_key, results, SEARCH_TTL)
        return results

    def get_band(self, profile_url: str) -> BandDetail:
        cache_key = profile_url.strip()
        cached = self.cache.read("band", cache_key, self._load_band_detail)
        if cached is not None:
            return cached

        with self._lock:
            page = self._require_page()
            try:
                with page.expect_response(
                    lambda response: "/band/discography/id/" in response.url and response.status == 200,
                    timeout=10_000,
                ):
                    page.goto(profile_url, wait_until="domcontentloaded")
            except TimeoutError:
                page.goto(profile_url, wait_until="domcontentloaded")

            self._ensure_not_challenge_page(page)
            detail = self._parse_band_detail(page.content(), profile_url)

        self.cache.write("band", cache_key, detail, BAND_TTL)
        return detail

    def get_album(self, album_url: str) -> AlbumDetail:
        cache_key = album_url.strip()
        cached = self.cache.read("album", cache_key, self._load_album_detail)
        if cached is not None:
            return cached

        with self._lock:
            page = self._require_page()
            page.goto(album_url, wait_until="domcontentloaded")
            self._ensure_not_challenge_page(page)

            try:
                page.wait_for_selector("table.table_lyrics", timeout=10_000)
            except TimeoutError:
                pass

            detail = self._parse_album_detail(page.content(), album_url)

        self.cache.write("album", cache_key, detail, ALBUM_TTL)
        return detail

    def _start_browser(self) -> None:
        self.playwright = sync_playwright().start()
        self._launch_persistent_browser(headless=not self.visible, use_saved_session=True)

        try:
            self._ensure_not_challenge_page(self._require_page())
        except CloudflareSessionError:
            self.close_browser_objects()
            if self._try_refresh_session_with_java():
                self._launch_persistent_browser(headless=not self.visible, use_saved_session=True)
                self._ensure_not_challenge_page(self._require_page())
            else:
                self._solve_cloudflare_and_save_session()

    def _resolve_firefox_executable(self) -> Path | None:
        default_path = Path(self.playwright.firefox.executable_path)
        if default_path.exists():
            return default_path

        cache_dir = Path.home() / ".cache" / "ms-playwright"
        candidates = sorted(cache_dir.glob("firefox-*/firefox/firefox"), reverse=True)
        for candidate in candidates:
            if candidate.exists():
                return candidate
        return None

    def _launch_persistent_browser(self, *, headless: bool, use_saved_session: bool) -> None:
        launch_kwargs = {"headless": headless}
        executable_path = self._resolve_firefox_executable()
        if executable_path is not None:
            launch_kwargs["executable_path"] = str(executable_path)

        try:
            self.browser = self.playwright.firefox.launch(**launch_kwargs)
        except Exception as exc:
            raise ScraperError(f"Could not launch Playwright Firefox: {exc}") from exc

        context_kwargs = {}
        if use_saved_session and self.session_file.exists():
            context_kwargs["storage_state"] = str(self.session_file)

        self.context = self.browser.new_context(**context_kwargs)
        self.page = self.context.new_page()
        self.page.goto(BASE_URL, wait_until="domcontentloaded")

    def _solve_cloudflare_and_save_session(self) -> None:
        self._launch_persistent_browser(headless=False, use_saved_session=False)
        page = self._require_page()

        if "moment" in page.title().lower():
            self._wait_for_cloudflare_resolution(timeout_ms=120_000)

        self.session_file.parent.mkdir(parents=True, exist_ok=True)
        self.context.storage_state(path=str(self.session_file))

        self.close_browser_objects()
        self._launch_persistent_browser(headless=not self.visible, use_saved_session=True)
        self._ensure_not_challenge_page(self._require_page())

    def _try_refresh_session_with_java(self) -> bool:
        if self._java_session_refresh_attempted:
            return False
        self._java_session_refresh_attempted = True

        jar_path = self._resolve_spring_jar()
        if jar_path is None:
            return False

        try:
            process = subprocess.run(
                [
                    "java",
                    "-jar",
                    str(jar_path),
                    "--ferrum.ui.type=refresh_session",
                    "--ferrum.browser.visible=true",
                ],
                check=False,
                text=True,
            )
        except Exception:
            return False

        return process.returncode == 0 and self.session_file.exists()

    def close_browser_objects(self) -> None:
        if self.context is not None:
            self.context.close()
            self.context = None
        if self.browser is not None:
            self.browser.close()
            self.browser = None
        self.page = None

    def _require_page(self) -> Page:
        if self.page is None:
            raise ScraperError("Browser page is not initialized.")
        return self.page

    def _resolve_spring_jar(self) -> Path | None:
        target_dir = SPRING_DIR / "target"
        if not target_dir.exists():
            return None

        candidates = sorted(
            path for path in target_dir.glob("ferrum-*.jar")
            if not path.name.endswith(".original")
        )
        return candidates[0] if candidates else None

    def _wait_for_cloudflare_resolution(self, timeout_ms: int) -> None:
        deadline = monotonic() + (timeout_ms / 1000)
        page = self._require_page()
        context = self.context
        if context is None:
            raise ScraperError("Browser context is not initialized.")

        while monotonic() < deadline:
            try:
                cookies = context.cookies([BASE_URL])
                if any(cookie.get("name") == "cf_clearance" and cookie.get("value") for cookie in cookies):
                    page.wait_for_load_state("domcontentloaded", timeout=5_000)
                    if "moment" not in page.title().lower():
                        return
            except Exception:
                pass

            page.wait_for_timeout(500)

        raise CloudflareSessionError(
            "Cloudflare challenge was not completed in time. Finish the human verification and try again."
        )

    def _ensure_not_challenge_page(self, page: Page) -> None:
        title = page.title().lower()
        if "moment" in title:
            raise CloudflareSessionError(
                "Metal Archives is behind Cloudflare. Refresh ~/.config/ferrum/session.json first."
            )

    def _parse_search_results(self, payload: dict | list, search_type: str) -> list[BandSummary]:
        rows = payload.get("aaData", []) if isinstance(payload, dict) else []
        results: list[BandSummary] = []
        for row in rows:
            if isinstance(row, list):
                results.append(self._parse_search_row(row, search_type))
        return results

    def _parse_search_row(self, row: list[object], search_type: str) -> BandSummary:
        column_texts: list[str] = []
        band_name = ""
        profile_url = ""

        for column in row:
            soup = BeautifulSoup(str(column), "html.parser")
            column_texts.append(soup.get_text(" ", strip=True))
            if not profile_url:
                anchor = soup.select_one('a[href*="/bands/"]')
                if anchor is not None:
                    band_name = anchor.get_text(" ", strip=True)
                    profile_url = anchor.get("href", "").strip()

        if not band_name:
            band_name = self._column_text(column_texts, 0)

        if search_type in {"BAND_NAME", "MUSIC_GENRE", "THEMES"}:
            return BandSummary(
                name=band_name,
                country=self._column_text(column_texts, 2),
                genre=self._column_text(column_texts, 1),
                status=self._column_text(column_texts, 3),
                profile_url=profile_url,
            )

        return BandSummary(
            name=band_name,
            country=self._column_text(column_texts, 1),
            genre=self._column_text(column_texts, 2),
            status=self._column_text(column_texts, 3),
            profile_url=profile_url,
        )

    def _parse_search_results_from_html(
        self,
        html: str,
        page_url: str,
        search_type: str,
    ) -> list[BandSummary]:
        soup = BeautifulSoup(html, "html.parser")
        results: list[BandSummary] = []

        for row in soup.select("table tbody tr"):
            cols = row.select("td")
            if len(cols) < 2:
                continue

            anchor = row.select_one('a[href*="/bands/"]')
            if anchor is None:
                continue

            texts = [col.get_text(" ", strip=True) for col in cols]
            band_name = anchor.get_text(" ", strip=True)
            profile_url = urljoin(page_url, anchor.get("href", "").strip())

            if search_type in {"BAND_NAME", "MUSIC_GENRE", "THEMES"}:
                genre = texts[1] if len(texts) > 1 else ""
                country = texts[2] if len(texts) > 2 else ""
                status = texts[3] if len(texts) > 3 else ""
            else:
                country = texts[1] if len(texts) > 1 else ""
                genre = texts[2] if len(texts) > 2 else ""
                status = texts[3] if len(texts) > 3 else ""

            results.append(
                BandSummary(
                    name=band_name,
                    country=country,
                    genre=genre,
                    status=status,
                    profile_url=profile_url,
                )
            )

        return results

    def _parse_band_detail(self, html: str, profile_url: str) -> BandDetail:
        soup = BeautifulSoup(html, "html.parser")
        band_name = self._text_or_empty(soup.select_one("h1.band_name"))

        country = ""
        location = ""
        status = ""
        formed_in = ""
        years_active = ""
        genre = ""
        lyrical_themes = ""
        label = ""

        stats = soup.select_one("div#band_stats")
        if stats is not None:
            for dt, dd in zip(stats.select("dt"), stats.select("dd")):
                key = dt.get_text(" ", strip=True).lower().replace(":", "")
                value = dd.get_text(" ", strip=True)
                if key == "country of origin":
                    country = value
                elif key == "location":
                    location = value
                elif key == "status":
                    status = value
                elif key == "formed in":
                    formed_in = value
                elif key == "years active":
                    years_active = value
                elif key == "genre":
                    genre = value
                elif key == "lyrical themes":
                    lyrical_themes = value
                elif key in {"current label", "last label"}:
                    label = value

        discography: list[AlbumEntry] = []
        for row in soup.select("div#band_disco table.discog tbody tr"):
            cols = row.select("td")
            if len(cols) < 3:
                continue
            anchor = cols[0].select_one("a")
            discography.append(
                AlbumEntry(
                    title=anchor.get_text(" ", strip=True) if anchor else cols[0].get_text(" ", strip=True),
                    type=cols[1].get_text(" ", strip=True),
                    year=cols[2].get_text(" ", strip=True),
                    url=urljoin(profile_url, anchor.get("href", "").strip()) if anchor else "",
                    image_url="",
                )
            )

        return BandDetail(
            name=band_name,
            image_url="",
            country=country,
            location=location,
            status=status,
            formed_in=formed_in,
            years_active=years_active,
            genre=genre,
            lyrical_themes=lyrical_themes,
            label=label,
            profile_url=profile_url,
            discography=discography,
        )

    def _parse_album_detail(self, html: str, album_url: str) -> AlbumDetail:
        soup = BeautifulSoup(html, "html.parser")
        title = self._text_or_empty(soup.select_one("h1.album_name"))

        release_type = ""
        release_date = ""
        label = ""

        info = soup.select_one("dl#album_info, div#album_info")
        if info is not None:
            for dt, dd in zip(info.select("dt"), info.select("dd")):
                key = dt.get_text(" ", strip=True).lower().replace(":", "")
                value = dd.get_text(" ", strip=True)
                if key == "type":
                    release_type = value
                elif key == "release date":
                    release_date = value
                elif key == "label":
                    label = value

        tracks: list[TrackEntry] = []
        table = soup.select_one("table.table_lyrics")
        if table is not None:
            for row in table.select("tbody tr"):
                cols = row.select("td")
                if len(cols) < 2:
                    continue
                number = cols[0].get_text(" ", strip=True)
                title_cell = BeautifulSoup(str(cols[1]), "html.parser")
                for removable in title_cell.select(
                    ".lyricsButton, a[title*=lyrics], a[href*=lyrics], span[id*=lyrics]"
                ):
                    removable.decompose()
                track_title = title_cell.get_text(" ", strip=True)
                if track_title.endswith("Show Lyrics"):
                    track_title = track_title.removesuffix("Show Lyrics").strip()
                duration = cols[2].get_text(" ", strip=True) if len(cols) >= 3 else ""
                if number and track_title:
                    tracks.append(TrackEntry(number=number, title=track_title, duration=duration))

        return AlbumDetail(
            title=title,
            image_url="",
            type=release_type,
            release_date=release_date,
            label=label,
            url=album_url,
            tracks=tracks,
        )

    def _load_band_summaries(self, payload: object) -> list[BandSummary]:
        rows = payload if isinstance(payload, list) else []
        return [BandSummary(**row) for row in rows if isinstance(row, dict)]

    def _load_band_detail(self, payload: object) -> BandDetail:
        if not isinstance(payload, dict):
            raise ValueError("Invalid band payload")
        discography = [
            AlbumEntry(
                title=item.get("title", ""),
                type=item.get("type", ""),
                year=item.get("year", ""),
                url=item.get("url", ""),
                image_url=item.get("image_url", ""),
            )
            for item in payload.get("discography", [])
            if isinstance(item, dict)
        ]
        return BandDetail(
            name=payload.get("name", ""),
            image_url=payload.get("image_url", ""),
            country=payload.get("country", ""),
            location=payload.get("location", ""),
            status=payload.get("status", ""),
            formed_in=payload.get("formed_in", ""),
            years_active=payload.get("years_active", ""),
            genre=payload.get("genre", ""),
            lyrical_themes=payload.get("lyrical_themes", ""),
            label=payload.get("label", ""),
            profile_url=payload.get("profile_url", ""),
            discography=discography,
        )

    def _load_album_detail(self, payload: object) -> AlbumDetail:
        if not isinstance(payload, dict):
            raise ValueError("Invalid album payload")
        tracks = [TrackEntry(**item) for item in payload.get("tracks", [])]
        return AlbumDetail(
            title=payload.get("title", ""),
            image_url=payload.get("image_url", ""),
            type=payload.get("type", ""),
            release_date=payload.get("release_date", ""),
            label=payload.get("label", ""),
            url=payload.get("url", ""),
            tracks=tracks,
        )

    def _normalize_query(self, query: str) -> str:
        return " ".join(query.strip().lower().split())

    def _column_text(self, values: list[str], index: int) -> str:
        return values[index] if index < len(values) else ""

    def _text_or_empty(self, node) -> str:
        return node.get_text(" ", strip=True) if node is not None else ""
