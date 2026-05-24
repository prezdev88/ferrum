package cl.tracktec.ferrum.application.port;

import cl.tracktec.ferrum.infrastructure.cache.CacheDescriptor;
import cl.tracktec.ferrum.infrastructure.cache.CachePayloadType;

import java.util.Optional;

public interface CacheStorePort {
    <T> Optional<T> read(CacheDescriptor descriptor, CachePayloadType<T> payloadType);

    <T> void write(CacheDescriptor descriptor, T payload);

    void delete(CacheDescriptor descriptor);
}
