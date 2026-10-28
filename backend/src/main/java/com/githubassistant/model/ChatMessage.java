package com.githubassistant.model;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name="messages", indexes={@Index(name="idx_messages_repository_created", columnList="repository_id,created_at")})
public class ChatMessage {
    @Id @Column(length=36, nullable=false) private String id = UUID.randomUUID().toString();
    @Column(name="repository_id", nullable=false, length=36) private String repositoryId;
    @Column(nullable=false, length=16) private String role;
    @Column(columnDefinition="TEXT", nullable=false) private String content;
    @Column(name="created_at", nullable=false) private Instant createdAt=Instant.now();
    public ChatMessage(){}
    public ChatMessage(String repositoryId,String role,String content){this.repositoryId=repositoryId;this.role=role;this.content=content;}
    public String getId(){return id;} public String getRepositoryId(){return repositoryId;} public String getRole(){return role;} public String getContent(){return content;} public Instant getCreatedAt(){return createdAt;}
}
