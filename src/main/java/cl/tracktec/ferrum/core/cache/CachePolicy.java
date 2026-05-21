package cl.tracktec.ferrum.core.cache;

import cl.tracktec.ferrum.core.domain.BandSearchType;
import cl.tracktec.ferrum.infrastructure.cache.FerrumCacheProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CachePolicy {

    private final FerrumCacheProperties properties;
    private final CacheKeyFactory keyFactory;

    public CachePolicy(FerrumCacheProperties properties, CacheKeyFactory keyFactory) {
        this.properties = properties;
        this.keyFactory = keyFactory;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public CacheDescriptor forSearch(String query, BandSearchType searchType) {
        String key = keyFactory.createSearchKey(query, searchType);
        Duration ttl = properties.getSearch().getTtl();
        return new CacheDescriptor(CacheNamespace.SEARCH, key, ttl);
    }

    public CacheDescriptor forBand(String profileUrl) {
        String key = keyFactory.createBandKey(profileUrl);
        Duration ttl = properties.getBand().getTtl();
        return new CacheDescriptor(CacheNamespace.BAND, key, ttl);
    }

    public CacheDescriptor forAlbum(String albumUrl) {
        String key = keyFactory.createAlbumKey(albumUrl);
        Duration ttl = properties.getAlbum().getTtl();
        return new CacheDescriptor(CacheNamespace.ALBUM, key, ttl);
    }
}
