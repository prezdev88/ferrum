package cl.tracktec.ferrum.core.cache;

import cl.tracktec.ferrum.core.domain.BandSearchType;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class CacheKeyFactory {

    public String createSearchKey(String query, BandSearchType searchType) {
        String normalizedQuery = normalizeQuery(query);
        return searchType.name().toLowerCase(Locale.ROOT) + "::" + normalizedQuery;
    }

    public String createBandKey(String profileUrl) {
        return profileUrl.trim();
    }

    public String createAlbumKey(String albumUrl) {
        return albumUrl.trim();
    }

    private String normalizeQuery(String query) {
        return query == null
                ? ""
                : query.trim()
                        .replaceAll("\\s+", " ")
                        .toLowerCase(Locale.ROOT);
    }
}
