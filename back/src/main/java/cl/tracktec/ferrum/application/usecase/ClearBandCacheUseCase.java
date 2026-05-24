package cl.tracktec.ferrum.application.usecase;

import cl.tracktec.ferrum.application.port.CacheStorePort;
import cl.tracktec.ferrum.domain.BandDetail;
import cl.tracktec.ferrum.infrastructure.cache.CacheDescriptor;
import cl.tracktec.ferrum.infrastructure.cache.CachePayloadType;
import cl.tracktec.ferrum.infrastructure.cache.CachePolicy;

import java.util.Optional;

public class ClearBandCacheUseCase {

    private static final CachePayloadType<BandDetail> BAND_PAYLOAD_TYPE = CachePayloadType.of(BandDetail.class);

    private final CacheStorePort cacheStore;
    private final CachePolicy cachePolicy;

    public ClearBandCacheUseCase(CacheStorePort cacheStore, CachePolicy cachePolicy) {
        this.cacheStore = cacheStore;
        this.cachePolicy = cachePolicy;
    }

    public void execute(String profileUrl) {
        CacheDescriptor bandDescriptor = cachePolicy.forBand(profileUrl);
        Optional<BandDetail> cachedBand = cacheStore.read(bandDescriptor, BAND_PAYLOAD_TYPE);

        cachedBand.ifPresent(bandDetail -> bandDetail.discography().stream()
                .map(BandDetail.AlbumEntry::url)
                .filter(url -> url != null && !url.isBlank())
                .map(cachePolicy::forAlbum)
                .forEach(this::deleteAlbumCache));

        cacheStore.delete(bandDescriptor);
    }

    private void deleteAlbumCache(CacheDescriptor descriptor) {
        cacheStore.delete(descriptor);
    }
}
