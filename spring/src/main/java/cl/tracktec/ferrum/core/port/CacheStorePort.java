package cl.tracktec.ferrum.core.port;

import cl.tracktec.ferrum.core.cache.CacheDescriptor;
import cl.tracktec.ferrum.core.cache.CachePayloadType;

import java.util.Optional;

public interface CacheStorePort {
    <T> Optional<T> read(CacheDescriptor descriptor, CachePayloadType<T> payloadType);

    <T> void write(CacheDescriptor descriptor, T payload);
}
