package cl.tracktec.ferrum.core.usecase;

import cl.tracktec.ferrum.core.domain.BandDetail;
import cl.tracktec.ferrum.core.port.BandSearchGateway;
import org.springframework.stereotype.Service;

@Service
public class GetBandDetailsUseCase {

    private final BandSearchGateway gateway;

    public GetBandDetailsUseCase(BandSearchGateway gateway) {
        this.gateway = gateway;
    }

    public BandDetail execute(String profileUrl) {
        return gateway.getDetails(profileUrl);
    }
}
