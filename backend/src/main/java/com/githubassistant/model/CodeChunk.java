package com.githubassistant.model;

import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name="code_chunks", indexes={
        @Index(name="idx_chunks_repository", columnList="repository_id"),
        @Index(name="idx_chunks_location", columnList="repository_id,file,start_line,end_line")})
public class CodeChunk {
    @Id @Column(length=36, nullable=false) private String id = UUID.randomUUID().toString();
    @Column(name="repository_id", nullable=false, length=36) private String repositoryId;
    @Column(nullable=false, length=2048) private String file;
    @Column(length=64) private String language;
    @Column(columnDefinition="TEXT", nullable=false) private String content;
    @Column(name="vector_id", length=128) private String vectorId;
    @Column(name="start_line", nullable=false) private int startLine;
    @Column(name="end_line", nullable=false) private int endLine;
    public CodeChunk(){}
    public CodeChunk(String repositoryId,String file,String language,String content,int startLine,int endLine){this.repositoryId=repositoryId;this.file=file;this.language=language;this.content=content;this.startLine=startLine;this.endLine=endLine;}
    public String getId(){return id;} public void setId(String v){id=v;}
    public String getRepositoryId(){return repositoryId;} public String getFile(){return file;} public String getLanguage(){return language;} public String getContent(){return content;} public String getVectorId(){return vectorId;} public void setVectorId(String v){vectorId=v;} public int getStartLine(){return startLine;} public int getEndLine(){return endLine;}
}
