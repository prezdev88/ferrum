from __future__ import annotations

import hashlib
import json
from dataclasses import asdict, is_dataclass
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Callable, TypeVar

T = TypeVar("T")


class FileCache:
    def __init__(self, base_directory: Path | None = None) -> None:
        self.base_directory = base_directory or Path.home() / ".config" / "ferrum" / "cache"

    def read(self, namespace: str, key: str, loader: Callable[[object], T]) -> T | None:
        cache_file = self._resolve_cache_file(namespace, key)
        if not cache_file.exists():
            return None

        try:
            payload = json.loads(cache_file.read_text(encoding="utf-8"))
            expires_at = datetime.fromisoformat(payload["expires_at"])
            if expires_at <= datetime.now(timezone.utc):
                cache_file.unlink(missing_ok=True)
                return None
            return loader(payload["payload"])
        except Exception:
            cache_file.unlink(missing_ok=True)
            return None

    def write(self, namespace: str, key: str, value: object, ttl: timedelta) -> None:
        cache_file = self._resolve_cache_file(namespace, key)
        cache_file.parent.mkdir(parents=True, exist_ok=True)

        cached_at = datetime.now(timezone.utc)
        payload = {
            "key": key,
            "cached_at": cached_at.isoformat(),
            "expires_at": (cached_at + ttl).isoformat(),
            "payload": self._serialize(value),
        }

        try:
            cache_file.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
        except OSError:
            pass

    def _resolve_cache_file(self, namespace: str, key: str) -> Path:
        hashed_key = hashlib.sha256(key.encode("utf-8")).hexdigest()
        return self.base_directory / namespace / f"{hashed_key}.json"

    def _serialize(self, value: object) -> object:
        if is_dataclass(value):
            return asdict(value)
        if isinstance(value, list):
            return [self._serialize(item) for item in value]
        return value
