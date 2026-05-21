package cl.tracktec.ferrum.core.port;

import cl.tracktec.ferrum.core.domain.AlbumDetail;
import cl.tracktec.ferrum.core.domain.BandDetail;
import cl.tracktec.ferrum.core.domain.BandSearchType;
import cl.tracktec.ferrum.core.domain.BandSummary;

import java.util.List;

public interface BandSearchGateway {
    List<BandSummary> search(String query, BandSearchType searchType);
    BandDetail getDetails(String profileUrl);
    AlbumDetail getAlbumDetails(String albumUrl);
}
