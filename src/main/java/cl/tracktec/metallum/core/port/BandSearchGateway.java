package cl.tracktec.metallum.core.port;

import cl.tracktec.metallum.core.domain.AlbumDetail;
import cl.tracktec.metallum.core.domain.BandDetail;
import cl.tracktec.metallum.core.domain.BandSummary;

import java.util.List;

public interface BandSearchGateway {
    List<BandSummary> searchByName(String name);
    BandDetail getDetails(String profileUrl);
    AlbumDetail getAlbumDetails(String albumUrl);
}
