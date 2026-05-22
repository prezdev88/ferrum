package cl.tracktec.ferrum.infrastructure.config;

import cl.tracktec.ferrum.application.port.BandSearchGateway;
import cl.tracktec.ferrum.application.usecase.GetAlbumDetailsUseCase;
import cl.tracktec.ferrum.application.usecase.GetBandDetailsUseCase;
import cl.tracktec.ferrum.application.usecase.SearchBandsUseCase;
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
}
