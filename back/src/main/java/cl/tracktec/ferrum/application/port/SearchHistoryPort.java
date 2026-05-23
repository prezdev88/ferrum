package cl.tracktec.ferrum.application.port;

import cl.tracktec.ferrum.application.model.SearchHistoryEntry;

import java.util.List;

public interface SearchHistoryPort {

    List<SearchHistoryEntry> loadRecentSearches(int limit);
}
