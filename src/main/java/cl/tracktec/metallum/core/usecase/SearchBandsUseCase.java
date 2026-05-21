package cl.tracktec.metallum.core.usecase;

import cl.tracktec.metallum.core.domain.BandSummary;
import cl.tracktec.metallum.core.domain.BandSearchType;
import cl.tracktec.metallum.core.port.BandSearchGateway;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchBandsUseCase {

    private final BandSearchGateway gateway;

    public SearchBandsUseCase(BandSearchGateway gateway) {
        this.gateway = gateway;
    }

    public List<BandSummary> execute(String query, BandSearchType searchType) {
        return gateway.search(query.trim(), searchType);
    }
}
