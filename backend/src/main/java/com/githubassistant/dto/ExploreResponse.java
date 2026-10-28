package com.githubassistant.dto;

import java.util.List;

public record ExploreResponse(
        int fileCount,
        int chunkCount,
        List<LanguageSummary> languages,
        List<AreaSummary> areas,
        List<EntryPoint> entryPoints,
        List<ExploreHit> results,
        List<FlowStage> flowStages) {
    public record LanguageSummary(String name, int files) {}
    public record AreaSummary(String name, String description, int files, List<String> paths) {}
    public record EntryPoint(String file, int line, String url) {}
    public record ExploreHit(String file, String language, int startLine, int endLine, String snippet, String url, String reason) {}
    public record FlowStage(String name, String explanation, List<ExploreHit> files) {}
}
