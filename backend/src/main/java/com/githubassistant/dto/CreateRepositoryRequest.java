package com.githubassistant.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record CreateRepositoryRequest(@NotBlank @Size(max = 2048) String url) {}
