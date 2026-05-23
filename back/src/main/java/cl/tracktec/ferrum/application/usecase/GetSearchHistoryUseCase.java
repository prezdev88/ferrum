package cl.tracktec.ferrum.application.usecase;

import cl.tracktec.ferrum.application.model.SearchHistoryEntry;
import cl.tracktec.ferrum.application.port.SearchHistoryPort;

import java.util.List;

public class GetSearchHistoryUseCase {

    private final SearchHistoryPort searchHistoryPort;

    public GetSearchHistoryUseCase(SearchHistoryPort searchHistoryPort) {
        this.searchHistoryPort = searchHistoryPort;
    }

    public List<SearchHistoryEntry> execute(int limit) {
        return searchHistoryPort.loadRecentSearches(limit);
    }
}
