package cl.tracktec.ferrum.core.cache;

import java.time.Instant;

public record CacheEntry<T>(
        String key,
        Instant cachedAt,
        Instant expiresAt,
        T payload
) {
    public boolean isExpiredAt(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
