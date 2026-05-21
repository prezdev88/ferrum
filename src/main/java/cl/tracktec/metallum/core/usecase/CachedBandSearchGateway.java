package cl.tracktec.metallum.core.usecase;

import cl.tracktec.metallum.core.cache.CacheDescriptor;
import cl.tracktec.metallum.core.cache.CachePayloadType;
import cl.tracktec.metallum.core.cache.CachePolicy;
import cl.tracktec.metallum.core.domain.AlbumDetail;
import cl.tracktec.metallum.core.domain.BandDetail;
import cl.tracktec.metallum.core.domain.BandSearchType;
import cl.tracktec.metallum.core.domain.BandSummary;
import cl.tracktec.metallum.core.port.BandSearchGateway;
import cl.tracktec.metallum.core.port.CacheStorePort;

import java.util.List;
import java.util.Optional;

public class CachedBandSearchGateway implements BandSearchGateway {

    private static final CachePayloadType<List<BandSummary>> SEARCH_PAYLOAD_TYPE =
            CachePayloadType.listOf(BandSummary.class);
    private static final CachePayloadType<BandDetail> BAND_PAYLOAD_TYPE =
            CachePayloadType.of(BandDetail.class);
    private static final CachePayloadType<AlbumDetail> ALBUM_PAYLOAD_TYPE =
            CachePayloadType.of(AlbumDetail.class);

    private final BandSearchGateway remoteGateway;
    private final CacheStorePort cacheStore;
    private final CachePolicy cachePolicy;

    public CachedBandSearchGateway(
            BandSearchGateway remoteGateway,
            CacheStorePort cacheStore,
            CachePolicy cachePolicy
    ) {
        this.remoteGateway = remoteGateway;
        this.cacheStore = cacheStore;
        this.cachePolicy = cachePolicy;
    }

    @Override
    public List<BandSummary> search(String query, BandSearchType searchType) {
        CacheDescriptor descriptor = cachePolicy.forSearch(query, searchType);
        List<BandSummary> remoteResults = loadFromCacheOrRemote(
                descriptor,
                SEARCH_PAYLOAD_TYPE,
                () -> remoteGateway.search(query, searchType)
        );
        return remoteResults;
    }

    @Override
    public BandDetail getDetails(String profileUrl) {
        CacheDescriptor descriptor = cachePolicy.forBand(profileUrl);
        BandDetail remoteDetails = loadFromCacheOrRemote(
                descriptor,
                BAND_PAYLOAD_TYPE,
                () -> remoteGateway.getDetails(profileUrl)
        );
        return remoteDetails;
    }

    @Override
    public AlbumDetail getAlbumDetails(String albumUrl) {
        CacheDescriptor descriptor = cachePolicy.forAlbum(albumUrl);
        AlbumDetail remoteAlbum = loadFromCacheOrRemote(
                descriptor,
                ALBUM_PAYLOAD_TYPE,
                () -> remoteGateway.getAlbumDetails(albumUrl)
        );
        return remoteAlbum;
    }

    private <T> T loadFromCacheOrRemote(
            CacheDescriptor descriptor,
            CachePayloadType<T> payloadType,
            CacheLoader<T> remoteLoader
    ) {
        if (!cachePolicy.isEnabled()) {
            return remoteLoader.load();
        }

        Optional<T> cachedPayload = cacheStore.read(descriptor, payloadType);
        if (cachedPayload.isPresent()) {
            return cachedPayload.get();
        }

        T remotePayload = remoteLoader.load();
        cacheStore.write(descriptor, remotePayload);
        return remotePayload;
    }

    @FunctionalInterface
    private interface CacheLoader<T> {
        T load();
    }
}
