package cl.tracktec.ferrum.core.cache;

import java.time.Duration;

public record CacheDescriptor(
        CacheNamespace namespace,
        String key,
        Duration ttl
) {
}
