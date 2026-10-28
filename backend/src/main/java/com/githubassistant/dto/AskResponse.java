package com.githubassistant.dto;
import java.util.List;
public record AskResponse(String answer,List<SourceReference> sources) {}
