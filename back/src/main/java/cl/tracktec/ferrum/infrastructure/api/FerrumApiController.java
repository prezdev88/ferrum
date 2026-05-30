package cl.tracktec.ferrum.infrastructure.api;

import cl.tracktec.ferrum.application.port.CacheStorePort;
import cl.tracktec.ferrum.application.model.SearchHistoryEntry;
import cl.tracktec.ferrum.application.usecase.ClearBandCacheUseCase;
import cl.tracktec.ferrum.application.usecase.GetAlbumDetailsUseCase;
import cl.tracktec.ferrum.application.usecase.GetBandDetailsUseCase;
import cl.tracktec.ferrum.application.usecase.GetSearchHistoryUseCase;
import cl.tracktec.ferrum.application.usecase.SearchBandsUseCase;
import cl.tracktec.ferrum.domain.AlbumDetail;
import cl.tracktec.ferrum.domain.BandDetail;
import cl.tracktec.ferrum.domain.BandSearchType;
import cl.tracktec.ferrum.domain.BandSummary;
import cl.tracktec.ferrum.infrastructure.cache.CachePayloadType;
import cl.tracktec.ferrum.infrastructure.cache.CachePolicy;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FerrumApiController {

    private static final CachePayloadType<BandDetail> BAND_PAYLOAD_TYPE = CachePayloadType.of(BandDetail.class);
    private static final CachePayloadType<AlbumDetail> ALBUM_PAYLOAD_TYPE = CachePayloadType.of(AlbumDetail.class);

    private final SearchBandsUseCase searchBands;
    private final GetBandDetailsUseCase getBandDetails;
    private final GetAlbumDetailsUseCase getAlbumDetails;
    private final GetSearchHistoryUseCase getSearchHistory;
    private final ClearBandCacheUseCase clearBandCache;
    private final CacheStorePort cacheStore;
    private final CachePolicy cachePolicy;

    public FerrumApiController(
            SearchBandsUseCase searchBands,
            GetBandDetailsUseCase getBandDetails,
            GetAlbumDetailsUseCase getAlbumDetails,
            GetSearchHistoryUseCase getSearchHistory,
            ClearBandCacheUseCase clearBandCache,
            CacheStorePort cacheStore,
            CachePolicy cachePolicy
    ) {
        this.searchBands = searchBands;
        this.getBandDetails = getBandDetails;
        this.getAlbumDetails = getAlbumDetails;
        this.getSearchHistory = getSearchHistory;
        this.clearBandCache = clearBandCache;
        this.cacheStore = cacheStore;
        this.cachePolicy = cachePolicy;
    }

    @GetMapping("/search")
    public List<BandSummary> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "BAND_NAME") String searchType
    ) {
        try {
            BandSearchType resolvedSearchType = parseSearchType(searchType);
            return searchBands.execute(query, resolvedSearchType);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage(), e);
        }
    }

    @GetMapping("/band")
    public BandDetail getBand(@RequestParam String url) {
        try {
            return getBandDetails.execute(url);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage(), e);
        }
    }

    @GetMapping("/album")
    public AlbumDetail getAlbum(@RequestParam String url) {
        try {
            return getAlbumDetails.execute(url);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage(), e);
        }
    }

    @GetMapping("/band-cache")
    public Map<String, Boolean> hasBandCache(@RequestParam String url) {
        boolean cached = cacheStore.read(cachePolicy.forBand(url), BAND_PAYLOAD_TYPE).isPresent();
        return Map.of("cached", cached);
    }

    @GetMapping("/album-cache")
    public Map<String, Boolean> hasAlbumCache(@RequestParam String url) {
        boolean cached = cacheStore.read(cachePolicy.forAlbum(url), ALBUM_PAYLOAD_TYPE).isPresent();
        return Map.of("cached", cached);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/search-history")
    public List<SearchHistoryEntry> getSearchHistory(
            @RequestParam(defaultValue = "100") int limit
    ) {
        return getSearchHistory.execute(limit);
    }

    @DeleteMapping("/band-cache")
    public Map<String, String> clearBandCache(@RequestParam String url) {
        try {
            clearBandCache.execute(url);
            return Map.of("status", "ok");
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage(), e);
        }
    }

    private BandSearchType parseSearchType(String rawValue) {
        String normalizedValue = rawValue.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        return BandSearchType.valueOf(normalizedValue);
    }
}
