package cl.tracktec.ferrum.infrastructure.config;

import cl.tracktec.ferrum.application.port.BandSearchGateway;
import cl.tracktec.ferrum.application.port.CacheStorePort;
import cl.tracktec.ferrum.infrastructure.cache.CachePolicy;
import cl.tracktec.ferrum.infrastructure.cache.CachedBandSearchGateway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class BandSearchGatewayConfiguration {

    @Bean
    @Primary
    public BandSearchGateway bandSearchGateway(
            @Qualifier("remoteBandSearchGateway") BandSearchGateway remoteGateway,
            CacheStorePort cacheStore,
            CachePolicy cachePolicy
    ) {
        return new CachedBandSearchGateway(remoteGateway, cacheStore, cachePolicy);
    }
}
