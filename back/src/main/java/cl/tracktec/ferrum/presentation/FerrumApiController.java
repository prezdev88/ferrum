package cl.tracktec.ferrum.presentation;

import cl.tracktec.ferrum.core.domain.AlbumDetail;
import cl.tracktec.ferrum.core.domain.BandDetail;
import cl.tracktec.ferrum.core.domain.BandSearchType;
import cl.tracktec.ferrum.core.domain.BandSummary;
import cl.tracktec.ferrum.core.usecase.GetAlbumDetailsUseCase;
import cl.tracktec.ferrum.core.usecase.GetBandDetailsUseCase;
import cl.tracktec.ferrum.core.usecase.SearchBandsUseCase;
import org.springframework.http.HttpStatus;
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

    private final SearchBandsUseCase searchBands;
    private final GetBandDetailsUseCase getBandDetails;
    private final GetAlbumDetailsUseCase getAlbumDetails;

    public FerrumApiController(
            SearchBandsUseCase searchBands,
            GetBandDetailsUseCase getBandDetails,
            GetAlbumDetailsUseCase getAlbumDetails
    ) {
        this.searchBands = searchBands;
        this.getBandDetails = getBandDetails;
        this.getAlbumDetails = getAlbumDetails;
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

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    private BandSearchType parseSearchType(String rawValue) {
        String normalizedValue = rawValue.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        return BandSearchType.valueOf(normalizedValue);
    }
}
