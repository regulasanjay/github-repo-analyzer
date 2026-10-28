package com.githubassistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExploreRequest(@NotBlank @Size(max = 1000) String query, String mode) {}
