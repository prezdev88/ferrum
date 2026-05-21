package cl.tracktec.metallum.core.domain;

import java.util.List;

public record AlbumDetail(
        String title,
        String type,
        String releaseDate,
        String label,
        String url,
        List<TrackEntry> tracks
) {
    public record TrackEntry(String number, String title, String duration) {}
}
