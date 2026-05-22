package cl.tracktec.ferrum.infrastructure.cache;

import java.time.Duration;

public record CacheDescriptor(
        CacheNamespace namespace,
        String key,
        Duration ttl
) {
}
