package cl.tracktec.metallum.core.domain;

public record BandSummary(
        String name,
        String country,
        String genre,
        String status,
        String profileUrl
) {}
