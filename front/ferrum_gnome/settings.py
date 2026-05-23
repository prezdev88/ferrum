from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path


@dataclass(slots=True)
class UserSettings:
    theme_mode: str = "black"
    music_provider: str = "youtube_music"


class SettingsStore:
    def __init__(self, file_path: Path | None = None) -> None:
        self.file_path = file_path or Path.home() / ".config" / "ferrum" / "preferences.json"

    def load(self) -> UserSettings:
        if not self.file_path.exists():
            return UserSettings()

        try:
            payload = json.loads(self.file_path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            return UserSettings()

        return UserSettings(
            theme_mode=payload.get("theme_mode", "black"),
            music_provider=payload.get("music_provider", "youtube_music"),
        )

    def save(self, settings: UserSettings) -> None:
        self.file_path.parent.mkdir(parents=True, exist_ok=True)
        payload = {
            "theme_mode": settings.theme_mode,
            "music_provider": settings.music_provider,
        }

        try:
            self.file_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
        except OSError:
            pass
