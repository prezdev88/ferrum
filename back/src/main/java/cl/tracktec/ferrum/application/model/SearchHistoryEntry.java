package cl.tracktec.ferrum.application.model;

import cl.tracktec.ferrum.domain.BandSearchType;

import java.time.Instant;

public record SearchHistoryEntry(
        String query,
        BandSearchType searchType,
        Instant cachedAt
) {
}
