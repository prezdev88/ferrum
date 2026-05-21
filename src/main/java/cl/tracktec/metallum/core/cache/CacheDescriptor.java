package cl.tracktec.metallum.core.cache;

import java.time.Duration;

public record CacheDescriptor(
        CacheNamespace namespace,
        String key,
        Duration ttl
) {
}
