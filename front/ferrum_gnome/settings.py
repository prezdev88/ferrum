from __future__ import annotations

import colorsys
import json
import random
import re
from dataclasses import dataclass, field
from pathlib import Path


def normalize_hex_color(value: str) -> str | None:
    if not value:
        return None

    normalized_value = value.strip().upper()
    if not re.fullmatch(r"#[0-9A-F]{6}", normalized_value):
        return None
    return normalized_value


def generate_random_color() -> str:
    hue = random.random()
    saturation = random.uniform(0.55, 0.82)
    value = random.uniform(0.72, 0.92)
    red, green, blue = colorsys.hsv_to_rgb(hue, saturation, value)
    return "#{:02X}{:02X}{:02X}".format(
        int(red * 255),
        int(green * 255),
        int(blue * 255),
    )


@dataclass(slots=True)
class UserSettings:
    theme_mode: str = "black"
    music_provider: str = "youtube_music"
    album_type_colors: dict[str, str] = field(default_factory=dict)


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

        raw_album_type_colors = payload.get("album_type_colors", {})
        album_type_colors: dict[str, str] = {}
        if isinstance(raw_album_type_colors, dict):
            for album_type, color in raw_album_type_colors.items():
                normalized_color = normalize_hex_color(str(color))
                normalized_type = str(album_type).strip()
                if normalized_type and normalized_color:
                    album_type_colors[normalized_type] = normalized_color

        return UserSettings(
            theme_mode=payload.get("theme_mode", "black"),
            music_provider=payload.get("music_provider", "youtube_music"),
            album_type_colors=album_type_colors,
        )

    def save(self, settings: UserSettings) -> None:
        self.file_path.parent.mkdir(parents=True, exist_ok=True)
        payload = {
            "theme_mode": settings.theme_mode,
            "music_provider": settings.music_provider,
            "album_type_colors": dict(sorted(settings.album_type_colors.items(), key=lambda item: item[0].casefold())),
        }

        try:
            self.file_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
        except OSError:
            pass
