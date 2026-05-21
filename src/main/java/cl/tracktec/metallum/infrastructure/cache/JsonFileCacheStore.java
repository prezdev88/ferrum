package cl.tracktec.metallum.infrastructure.cache;

import cl.tracktec.metallum.core.cache.CacheDescriptor;
import cl.tracktec.metallum.core.cache.CacheEntry;
import cl.tracktec.metallum.core.cache.CacheNamespace;
import cl.tracktec.metallum.core.cache.CachePayloadType;
import cl.tracktec.metallum.core.port.CacheStorePort;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class JsonFileCacheStore implements CacheStorePort {

    private static final String FILE_EXTENSION = ".json";
    private static final int MAX_FILES_TO_CLEAN_PER_WRITE = 20;

    private final ObjectMapper objectMapper;
    private final MetallumCacheProperties properties;

    public JsonFileCacheStore(ObjectMapper objectMapper, MetallumCacheProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public <T> Optional<T> read(CacheDescriptor descriptor, CachePayloadType<T> payloadType) {
        try {
            Path cacheFile = resolveCacheFile(descriptor.namespace(), descriptor.key());
            if (!Files.exists(cacheFile)) {
                return Optional.empty();
            }

            Instant now = Instant.now();
            CacheEntry<T> entry = readEntry(cacheFile, descriptor, payloadType);
            if (entry.isExpiredAt(now)) {
                deleteQuietly(cacheFile);
                return Optional.empty();
            }

            return Optional.ofNullable(entry.payload());
        } catch (IOException e) {
            deleteQuietly(resolveCacheFile(descriptor.namespace(), descriptor.key()));
            return Optional.empty();
        }
    }

    @Override
    public <T> void write(CacheDescriptor descriptor, T payload) {
        try {
            Path cacheFile = resolveCacheFile(descriptor.namespace(), descriptor.key());
            Files.createDirectories(cacheFile.getParent());

            Instant cachedAt = Instant.now();
            CacheEntry<T> entry = new CacheEntry<>(
                    descriptor.key(),
                    cachedAt,
                    cachedAt.plus(descriptor.ttl()),
                    payload
            );

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(cacheFile.toFile(), entry);
            cleanupExpiredEntries(descriptor.namespace(), cachedAt);
        } catch (IOException ignored) {
            // Cache write failures must not break the remote flow.
        }
    }

    private <T> CacheEntry<T> readEntry(
            Path cacheFile,
            CacheDescriptor descriptor,
            CachePayloadType<T> payloadType
    ) throws IOException {
        JavaType entryType = buildEntryType(payloadType);
        CacheEntry<T> entry = objectMapper.readValue(cacheFile.toFile(), entryType);
        if (!descriptor.key().equals(entry.key())) {
            deleteQuietly(cacheFile);
            throw new IOException("Cache key mismatch");
        }
        return entry;
    }

    private JavaType buildEntryType(CachePayloadType<?> payloadType) {
        JavaType payloadJavaType = buildPayloadType(payloadType);
        return objectMapper.getTypeFactory().constructParametricType(CacheEntry.class, payloadJavaType);
    }

    private JavaType buildPayloadType(CachePayloadType<?> payloadType) {
        if (!payloadType.isCollection()) {
            return objectMapper.getTypeFactory().constructType(payloadType.rawClass());
        }

        return objectMapper.getTypeFactory()
                .constructCollectionType(payloadType.collectionClass(), payloadType.itemClass());
    }

    private void cleanupExpiredEntries(CacheNamespace namespace, Instant now) throws IOException {
        Path namespaceDirectory = resolveNamespaceDirectory(namespace);
        if (!Files.isDirectory(namespaceDirectory)) {
            return;
        }

        try (Stream<Path> files = Files.list(namespaceDirectory)) {
            files.filter(Files::isRegularFile)
                    .limit(MAX_FILES_TO_CLEAN_PER_WRITE)
                    .forEach(path -> deleteIfExpired(path, now));
        }
    }

    private void deleteIfExpired(Path cacheFile, Instant now) {
        try {
            JavaType entryType = objectMapper.getTypeFactory().constructParametricType(CacheEntry.class, Object.class);
            CacheEntry<?> entry = objectMapper.readValue(cacheFile.toFile(), entryType);
            if (entry.isExpiredAt(now)) {
                deleteQuietly(cacheFile);
            }
        } catch (IOException e) {
            deleteQuietly(cacheFile);
        }
    }

    private Path resolveCacheFile(CacheNamespace namespace, String key) {
        String hashedKey = hashKey(key);
        return resolveNamespaceDirectory(namespace).resolve(hashedKey + FILE_EXTENSION);
    }

    private Path resolveNamespaceDirectory(CacheNamespace namespace) {
        return properties.getDirectory().resolve(namespace.getDirectoryName());
    }

    private String hashKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best effort cleanup.
        }
    }
}
