package cl.tracktec.ferrum.domain;

public record BandSummary(
        String name,
        String country,
        String genre,
        String status,
        String profileUrl
) {}
