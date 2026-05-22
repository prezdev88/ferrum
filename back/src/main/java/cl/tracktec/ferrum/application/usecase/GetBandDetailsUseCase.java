package cl.tracktec.ferrum.application.usecase;

import cl.tracktec.ferrum.application.port.BandSearchGateway;
import cl.tracktec.ferrum.domain.BandDetail;

public class GetBandDetailsUseCase {

    private final BandSearchGateway gateway;

    public GetBandDetailsUseCase(BandSearchGateway gateway) {
        this.gateway = gateway;
    }

    public BandDetail execute(String profileUrl) {
        return gateway.getDetails(profileUrl);
    }
}
