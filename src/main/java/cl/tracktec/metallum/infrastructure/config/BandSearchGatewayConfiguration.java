package cl.tracktec.metallum.infrastructure.config;

import cl.tracktec.metallum.core.cache.CachePolicy;
import cl.tracktec.metallum.core.port.BandSearchGateway;
import cl.tracktec.metallum.core.port.CacheStorePort;
import cl.tracktec.metallum.core.usecase.CachedBandSearchGateway;
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
