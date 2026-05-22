package cl.tracktec.ferrum.application.port;

import cl.tracktec.ferrum.domain.AlbumDetail;
import cl.tracktec.ferrum.domain.BandDetail;
import cl.tracktec.ferrum.domain.BandSearchType;
import cl.tracktec.ferrum.domain.BandSummary;

import java.util.List;

public interface BandSearchGateway {
    List<BandSummary> search(String query, BandSearchType searchType);
    BandDetail getDetails(String profileUrl);
    AlbumDetail getAlbumDetails(String albumUrl);
}
