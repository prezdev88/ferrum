from __future__ import annotations

import json
import os
import time
from urllib.parse import urlencode

import requests
from .models import AlbumDetail, AlbumEntry, BandDetail, BandSummary, TrackEntry


class BackendError(RuntimeError):
    pass


class FerrumBackend:
    DEFAULT_BASE_URL = "http://localhost:8080"
    REQUEST_TIMEOUT_SECONDS = 90
    HEALTH_TIMEOUT_SECONDS = 2

    def __init__(self, base_url: str | None = None) -> None:
        resolvedBaseUrl = base_url or os.environ.get("FERRUM_BACKEND_URL") or self.DEFAULT_BASE_URL
        self.baseUrl = resolvedBaseUrl.rstrip("/")
        self.http = requests.Session()

    def search(self, query: str, search_type: str) -> list[BandSummary]:
        payload = self._get_json("/api/search", {"query": query, "searchType": search_type})
        return [
            BandSummary(
                name=item.get("name", ""),
                country=item.get("country", ""),
                genre=item.get("genre", ""),
                status=item.get("status", ""),
                profile_url=item.get("profileUrl", ""),
            )
            for item in payload
        ]

    def get_band(self, profile_url: str) -> BandDetail:
        payload = self._get_json("/api/band", {"url": profile_url})
        return BandDetail(
            name=payload.get("name", ""),
            image_url=payload.get("imageUrl", ""),
            country=payload.get("country", ""),
            location=payload.get("location", ""),
            status=payload.get("status", ""),
            formed_in=payload.get("formedIn", ""),
            years_active=payload.get("yearsActive", ""),
            genre=payload.get("genre", ""),
            lyrical_themes=payload.get("lyricalThemes", ""),
            label=payload.get("label", ""),
            profile_url=payload.get("profileUrl", ""),
            discography=[
                AlbumEntry(
                    title=item.get("title", ""),
                    type=item.get("type", ""),
                    year=item.get("year", ""),
                    url=item.get("url", ""),
                    image_url=item.get("imageUrl", ""),
                )
                for item in payload.get("discography", [])
            ],
        )

    def get_album(self, album_url: str) -> AlbumDetail:
        payload = self._get_json("/api/album", {"url": album_url})
        return AlbumDetail(
            title=payload.get("title", ""),
            image_url=payload.get("imageUrl", ""),
            type=payload.get("type", ""),
            release_date=payload.get("releaseDate", ""),
            label=payload.get("label", ""),
            url=payload.get("url", ""),
            tracks=[
                TrackEntry(
                    number=item.get("number", ""),
                    title=item.get("title", ""),
                    duration=item.get("duration", ""),
                )
                for item in payload.get("tracks", [])
            ],
        )

    def wait_until_ready(self, timeout_seconds: int = 60) -> None:
        deadline = time.time() + timeout_seconds
        last_error: Exception | None = None

        while time.time() < deadline:
            try:
                if self.is_ready():
                    return
            except BackendError as exc:
                last_error = exc
            time.sleep(0.5)

        if last_error is not None:
            raise last_error
        raise BackendError("Ferrum backend did not become ready in time.")

    def is_ready(self) -> bool:
        url = f"{self.baseUrl}/api/health"

        try:
            response = self.http.get(url, timeout=self.HEALTH_TIMEOUT_SECONDS)
        except requests.RequestException as exc:
            raise BackendError(
                "Could not reach the Ferrum backend API at "
                + self.baseUrl
                + ". Start the Spring backend first."
            ) from exc

        if response.status_code >= 400:
            raise BackendError(self._normalize_error_message(response))

        try:
            payload = response.json()
        except json.JSONDecodeError as exc:
            raise BackendError(f"Invalid backend health response: {exc}") from exc

        return payload.get("status") == "ok"

    def _get_json(self, path: str, params: dict[str, str]):
        queryString = urlencode(params)
        url = f"{self.baseUrl}{path}?{queryString}"

        try:
            response = self.http.get(url, timeout=self.REQUEST_TIMEOUT_SECONDS)
        except requests.RequestException as exc:
            raise BackendError(
                "Could not reach the Ferrum backend API at "
                + self.baseUrl
                + ". Start the Spring backend first."
            ) from exc

        if response.status_code >= 400:
            raise BackendError(self._normalize_error_message(response))

        try:
            return response.json()
        except json.JSONDecodeError as exc:
            raise BackendError(f"Invalid backend response: {exc}") from exc

    def _normalize_error_message(self, response: requests.Response) -> str:
        message = response.text.strip()
        compact = " ".join(message.split())

        if response.status_code == 400:
            return "Invalid backend request. Check the query parameters."
        if "Host system is missing dependencies to run browsers" in message:
            return (
                "The Java scraper backend cannot start its Playwright browser because "
                "system libraries are missing."
            )
        if "moment" in compact.lower():
            return (
                "Metal Archives is still behind Cloudflare. Complete the Java browser verification "
                "when prompted and retry."
            )
        if response.status_code == 502:
            return compact[:800] or "Backend scraper request failed."
        if response.status_code >= 500:
            return "Ferrum backend API failed while processing the request."
        return compact[:800]
