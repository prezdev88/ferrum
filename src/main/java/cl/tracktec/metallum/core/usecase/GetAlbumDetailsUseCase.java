package cl.tracktec.metallum.core.usecase;

import cl.tracktec.metallum.core.domain.AlbumDetail;
import cl.tracktec.metallum.core.port.BandSearchGateway;
import org.springframework.stereotype.Service;

@Service
public class GetAlbumDetailsUseCase {

    private final BandSearchGateway gateway;

    public GetAlbumDetailsUseCase(BandSearchGateway gateway) {
        this.gateway = gateway;
    }

    public AlbumDetail execute(String albumUrl) {
        return gateway.getAlbumDetails(albumUrl);
    }
}
