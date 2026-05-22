package cl.tracktec.ferrum.infrastructure.cache;

import cl.tracktec.ferrum.application.port.BandSearchGateway;
import cl.tracktec.ferrum.application.port.CacheStorePort;
import cl.tracktec.ferrum.domain.AlbumDetail;
import cl.tracktec.ferrum.domain.BandDetail;
import cl.tracktec.ferrum.domain.BandSearchType;
import cl.tracktec.ferrum.domain.BandSummary;

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
        return enrichDiscographyWithCachedAlbumImages(remoteDetails);
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

    private BandDetail enrichDiscographyWithCachedAlbumImages(BandDetail bandDetail) {
        List<BandDetail.AlbumEntry> originalDiscography = bandDetail.discography();
        List<BandDetail.AlbumEntry> enrichedDiscography = originalDiscography.stream()
                .map(this::enrichAlbumEntryWithCachedImage)
                .toList();

        return new BandDetail(
                bandDetail.name(),
                bandDetail.imageUrl(),
                bandDetail.country(),
                bandDetail.location(),
                bandDetail.status(),
                bandDetail.formedIn(),
                bandDetail.yearsActive(),
                bandDetail.genre(),
                bandDetail.lyricalThemes(),
                bandDetail.label(),
                bandDetail.profileUrl(),
                enrichedDiscography
        );
    }

    private BandDetail.AlbumEntry enrichAlbumEntryWithCachedImage(BandDetail.AlbumEntry albumEntry) {
        if (albumEntry.url().isBlank()) {
            return albumEntry;
        }

        CacheDescriptor descriptor = cachePolicy.forAlbum(albumEntry.url());
        Optional<AlbumDetail> cachedAlbum = cacheStore.read(descriptor, ALBUM_PAYLOAD_TYPE);
        if (cachedAlbum.isEmpty()) {
            return albumEntry;
        }

        String imageUrl = cachedAlbum.get().imageUrl();
        return new BandDetail.AlbumEntry(
                albumEntry.title(),
                albumEntry.type(),
                albumEntry.year(),
                albumEntry.url(),
                imageUrl
        );
    }

    @FunctionalInterface
    private interface CacheLoader<T> {
        T load();
    }
}
