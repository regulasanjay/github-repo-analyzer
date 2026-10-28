package com.githubassistant.model;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name="search_history", indexes={@Index(name="idx_search_history_repository_created", columnList="repository_id,created_at")})
public class SearchHistory {
    @Id @Column(length=36, nullable=false) private String id = UUID.randomUUID().toString();
    @Column(name="repository_id", nullable=false, length=36) private String repositoryId;
    @Column(nullable=false, length=2000) private String query;
    @Column(nullable=false, length=16) private String mode;
    @Column(name="result_count", nullable=false) private int resultCount;
    @Column(name="created_at", nullable=false) private Instant createdAt = Instant.now();

    public SearchHistory() {}
    public SearchHistory(String repositoryId, String query, String mode, int resultCount) {
        this.repositoryId = repositoryId; this.query = query; this.mode = mode; this.resultCount = resultCount;
    }
    public String getId() { return id; }
    public String getRepositoryId() { return repositoryId; }
    public String getQuery() { return query; }
    public String getMode() { return mode; }
    public int getResultCount() { return resultCount; }
    public Instant getCreatedAt() { return createdAt; }
}
