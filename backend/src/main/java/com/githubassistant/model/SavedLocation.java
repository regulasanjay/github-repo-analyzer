package com.githubassistant.model;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name="saved_locations", indexes={@Index(name="idx_saved_locations_repository_created", columnList="repository_id,created_at")})
public class SavedLocation {
    @Id @Column(length=36, nullable=false) private String id = UUID.randomUUID().toString();
    @Column(name="repository_id", nullable=false, length=36) private String repositoryId;
    @Column(nullable=false, length=2048) private String file;
    @Column(columnDefinition="TEXT", nullable=false) private String snippet;
    @Column(nullable=false, length=4096) private String url;
    @Column(columnDefinition="TEXT") private String note;
    @Column(name="start_line", nullable=false) private int startLine;
    @Column(name="end_line", nullable=false) private int endLine;
    @Column(name="created_at", nullable=false) private Instant createdAt = Instant.now();

    public SavedLocation() {}
    public SavedLocation(String repositoryId, String file, int startLine, int endLine, String snippet, String url, String note) {
        this.repositoryId=repositoryId; this.file=file; this.startLine=startLine; this.endLine=endLine;
        this.snippet=snippet; this.url=url; this.note=note;
    }
    public String getId(){return id;} public String getRepositoryId(){return repositoryId;} public String getFile(){return file;}
    public String getSnippet(){return snippet;} public String getUrl(){return url;} public String getNote(){return note;}
    public int getStartLine(){return startLine;} public int getEndLine(){return endLine;} public Instant getCreatedAt(){return createdAt;}
}
