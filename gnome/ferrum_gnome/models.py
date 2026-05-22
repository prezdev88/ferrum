from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(slots=True)
class BandSummary:
    name: str
    country: str
    genre: str
    status: str
    profile_url: str


@dataclass(slots=True)
class AlbumEntry:
    title: str
    type: str
    year: str
    url: str


@dataclass(slots=True)
class BandDetail:
    name: str
    country: str
    location: str
    status: str
    formed_in: str
    years_active: str
    genre: str
    lyrical_themes: str
    label: str
    profile_url: str
    discography: list[AlbumEntry] = field(default_factory=list)


@dataclass(slots=True)
class TrackEntry:
    number: str
    title: str
    duration: str


@dataclass(slots=True)
class AlbumDetail:
    title: str
    type: str
    release_date: str
    label: str
    url: str
    tracks: list[TrackEntry] = field(default_factory=list)
