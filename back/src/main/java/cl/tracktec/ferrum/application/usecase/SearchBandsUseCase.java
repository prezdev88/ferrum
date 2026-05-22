package cl.tracktec.ferrum.application.usecase;

import cl.tracktec.ferrum.application.port.BandSearchGateway;
import cl.tracktec.ferrum.domain.BandSearchType;
import cl.tracktec.ferrum.domain.BandSummary;

import java.util.List;

public class SearchBandsUseCase {

    private final BandSearchGateway gateway;

    public SearchBandsUseCase(BandSearchGateway gateway) {
        this.gateway = gateway;
    }

    public List<BandSummary> execute(String query, BandSearchType searchType) {
        return gateway.search(query.trim(), searchType);
    }
}
