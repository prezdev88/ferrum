package cl.tracktec.ferrum.infrastructure.config;

import cl.tracktec.ferrum.application.port.BandSearchGateway;
import cl.tracktec.ferrum.application.port.CacheStorePort;
import cl.tracktec.ferrum.application.port.SearchHistoryPort;
import cl.tracktec.ferrum.application.usecase.ClearBandCacheUseCase;
import cl.tracktec.ferrum.application.usecase.GetAlbumDetailsUseCase;
import cl.tracktec.ferrum.application.usecase.GetBandDetailsUseCase;
import cl.tracktec.ferrum.application.usecase.GetSearchHistoryUseCase;
import cl.tracktec.ferrum.application.usecase.SearchBandsUseCase;
import cl.tracktec.ferrum.infrastructure.cache.CachePolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationUseCaseConfiguration {

    @Bean
    public SearchBandsUseCase searchBandsUseCase(BandSearchGateway bandSearchGateway) {
        return new SearchBandsUseCase(bandSearchGateway);
    }

    @Bean
    public GetBandDetailsUseCase getBandDetailsUseCase(BandSearchGateway bandSearchGateway) {
        return new GetBandDetailsUseCase(bandSearchGateway);
    }

    @Bean
    public GetAlbumDetailsUseCase getAlbumDetailsUseCase(BandSearchGateway bandSearchGateway) {
        return new GetAlbumDetailsUseCase(bandSearchGateway);
    }

    @Bean
    public GetSearchHistoryUseCase getSearchHistoryUseCase(SearchHistoryPort searchHistoryPort) {
        return new GetSearchHistoryUseCase(searchHistoryPort);
    }

    @Bean
    public ClearBandCacheUseCase clearBandCacheUseCase(CacheStorePort cacheStore, CachePolicy cachePolicy) {
        return new ClearBandCacheUseCase(cacheStore, cachePolicy);
    }
}
