package cl.tracktec.ferrum.core.domain;

import java.util.List;

public record BandDetail(
        String name,
        String country,
        String location,
        String status,
        String formedIn,
        String yearsActive,
        String genre,
        String lyricalThemes,
        String label,
        String profileUrl,
        List<AlbumEntry> discography
) {
    public record AlbumEntry(String title, String type, String year, String url) {}
}
