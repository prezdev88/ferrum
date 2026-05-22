package cl.tracktec.ferrum.infrastructure.cache;

import java.util.Collection;

public record CachePayloadType<T>(
        Class<?> rawClass,
        Class<?> itemClass
) {
    public static <T> CachePayloadType<T> of(Class<T> type) {
        return new CachePayloadType<>(type, null);
    }

    public static <T> CachePayloadType<T> listOf(Class<?> itemClass) {
        return new CachePayloadType<>(java.util.List.class, itemClass);
    }

    public boolean isCollection() {
        return itemClass != null;
    }

    @SuppressWarnings("unchecked")
    public Class<? extends Collection> collectionClass() {
        return (Class<? extends Collection>) rawClass;
    }
}
