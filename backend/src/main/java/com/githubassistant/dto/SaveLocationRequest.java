package com.githubassistant.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SaveLocationRequest(@NotBlank String file, @Min(1) int startLine, @Min(1) int endLine, String note) {}
