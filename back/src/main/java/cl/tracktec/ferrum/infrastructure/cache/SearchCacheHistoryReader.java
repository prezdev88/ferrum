package cl.tracktec.ferrum.infrastructure.cache;

import cl.tracktec.ferrum.application.model.SearchHistoryEntry;
import cl.tracktec.ferrum.application.port.SearchHistoryPort;
import cl.tracktec.ferrum.domain.BandSearchType;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Component
public class SearchCacheHistoryReader implements SearchHistoryPort {

    private static final String SEARCH_KEY_SEPARATOR = "::";

    private final ObjectMapper objectMapper;
    private final FerrumCacheProperties cacheProperties;

    public SearchCacheHistoryReader(ObjectMapper objectMapper, FerrumCacheProperties cacheProperties) {
        this.objectMapper = objectMapper;
        this.cacheProperties = cacheProperties;
    }

    @Override
    public List<SearchHistoryEntry> loadRecentSearches(int limit) {
        Path searchCacheDirectory = resolveSearchCacheDirectory();
        if (!Files.isDirectory(searchCacheDirectory)) {
            return List.of();
        }

        int resolvedLimit = Math.max(1, limit);
        try (Stream<Path> files = Files.list(searchCacheDirectory)) {
            return files.filter(Files::isRegularFile)
                    .map(this::readSearchHistoryEntry)
                    .filter(entry -> entry != null)
                    .sorted(Comparator.comparing(SearchHistoryEntry::cachedAt).reversed())
                    .limit(resolvedLimit)
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private Path resolveSearchCacheDirectory() {
        return cacheProperties.getDirectory().resolve(CacheNamespace.SEARCH.getDirectoryName());
    }

    private SearchHistoryEntry readSearchHistoryEntry(Path cacheFile) {
        try {
            JavaType entryType = objectMapper.getTypeFactory()
                    .constructParametricType(CacheEntry.class, Object.class);
            CacheEntry<?> cacheEntry = objectMapper.readValue(cacheFile.toFile(), entryType);
            Instant now = Instant.now();
            if (cacheEntry.isExpiredAt(now)) {
                deleteQuietly(cacheFile);
                return null;
            }
            return parseSearchHistoryEntry(cacheEntry);
        } catch (IOException e) {
            deleteQuietly(cacheFile);
            return null;
        }
    }

    private SearchHistoryEntry parseSearchHistoryEntry(CacheEntry<?> cacheEntry) {
        String cacheKey = cacheEntry.key();
        int separatorIndex = cacheKey.indexOf(SEARCH_KEY_SEPARATOR);
        if (separatorIndex < 0) {
            return null;
        }

        String rawSearchType = cacheKey.substring(0, separatorIndex);
        String query = cacheKey.substring(separatorIndex + SEARCH_KEY_SEPARATOR.length()).trim();
        if (query.isBlank()) {
            return null;
        }

        BandSearchType searchType = parseSearchType(rawSearchType);
        if (searchType == null) {
            return null;
        }

        return new SearchHistoryEntry(query, searchType, cacheEntry.cachedAt());
    }

    private BandSearchType parseSearchType(String rawSearchType) {
        try {
            String normalizedSearchType = rawSearchType.trim()
                    .toUpperCase(Locale.ROOT)
                    .replace('-', '_')
                    .replace(' ', '_');
            return BandSearchType.valueOf(normalizedSearchType);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void deleteQuietly(Path cacheFile) {
        try {
            Files.deleteIfExists(cacheFile);
        } catch (IOException ignored) {
            // Best effort cleanup.
        }
    }
}
