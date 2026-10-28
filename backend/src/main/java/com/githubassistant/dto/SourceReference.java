package com.githubassistant.dto;
public record SourceReference(String file,int startLine,int endLine,String url,String snippet) {}
