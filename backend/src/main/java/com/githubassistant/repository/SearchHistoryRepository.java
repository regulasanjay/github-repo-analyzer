package com.githubassistant.repository;

import com.githubassistant.model.SearchHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, String> {
    List<SearchHistory> findTop12ByRepositoryIdOrderByCreatedAtDesc(String repositoryId);
}
