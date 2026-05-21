package cl.tracktec.metallum.core.port;

import cl.tracktec.metallum.core.cache.CacheDescriptor;
import cl.tracktec.metallum.core.cache.CachePayloadType;

import java.util.Optional;

public interface CacheStorePort {
    <T> Optional<T> read(CacheDescriptor descriptor, CachePayloadType<T> payloadType);

    <T> void write(CacheDescriptor descriptor, T payload);
}
