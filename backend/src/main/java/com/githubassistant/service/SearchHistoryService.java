package com.githubassistant.service;

import com.githubassistant.model.SearchHistory;
import com.githubassistant.repository.SearchHistoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SearchHistoryService {
    private final SearchHistoryRepository history;
    public SearchHistoryService(SearchHistoryRepository history) { this.history = history; }
    public SearchHistory record(String repositoryId, String query, String mode, int resultCount) {
        return history.save(new SearchHistory(repositoryId, query.trim(), mode, resultCount));
    }
    public List<SearchHistory> recent(String repositoryId) {
        return history.findTop12ByRepositoryIdOrderByCreatedAtDesc(repositoryId);
    }
}
