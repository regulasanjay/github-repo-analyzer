package com.githubassistant.model;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name="repositories", indexes={@Index(name="idx_repositories_url", columnList="url")})
public class RepositoryRecord {
    @Id @Column(length=36, nullable=false) private String id = UUID.randomUUID().toString();
    @Column(nullable=false, length=2048) private String url;
    @Column(nullable=false, length=255) private String owner;
    @Column(nullable=false, length=255) private String name;
    @Column(name="default_branch", nullable=false, length=255) private String defaultBranch = "main";
    @Column(nullable=false, length=32) private String status = "processing";
    @Column(length=2000) private String error;
    @Column(name="file_count", nullable=false) private int fileCount;
    @Column(name="chunk_count", nullable=false) private int chunkCount;
    @Column(name="created_at", nullable=false) private Instant createdAt = Instant.now();
    @Column(name="updated_at", nullable=false) private Instant updatedAt = Instant.now();
    public String getId(){return id;} public void setId(String v){id=v;}
    public String getUrl(){return url;} public void setUrl(String v){url=v;}
    public String getOwner(){return owner;} public void setOwner(String v){owner=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getDefaultBranch(){return defaultBranch;} public void setDefaultBranch(String v){defaultBranch=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getError(){return error;} public void setError(String v){error=v;}
    public int getFileCount(){return fileCount;} public void setFileCount(int v){fileCount=v;}
    public int getChunkCount(){return chunkCount;} public void setChunkCount(int v){chunkCount=v;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
    public void touch(){updatedAt=Instant.now();}
}
