package cl.tracktec.ferrum.application.usecase;

import cl.tracktec.ferrum.application.port.BandSearchGateway;
import cl.tracktec.ferrum.domain.AlbumDetail;

public class GetAlbumDetailsUseCase {

    private final BandSearchGateway gateway;

    public GetAlbumDetailsUseCase(BandSearchGateway gateway) {
        this.gateway = gateway;
    }

    public AlbumDetail execute(String albumUrl) {
        return gateway.getAlbumDetails(albumUrl);
    }
}
