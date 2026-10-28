package com.githubassistant.service;

import com.githubassistant.dto.ExploreResponse;
import com.githubassistant.model.CodeChunk;
import com.githubassistant.model.RepositoryRecord;
import com.githubassistant.repository.CodeChunkRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RepositoryExplorerService {
    private final CodeChunkRepository chunks;
    private final SearchService search;

    private static final Map<String, String> AREA_DESCRIPTIONS = Map.of(
            "Product features", "Application behavior and domain logic",
            "API and services", "HTTP endpoints, controllers, and service layer",
            "User interface", "Screens, components, and browser behavior",
            "Data and storage", "Database models, queries, migrations, and persistence",
            "Tests", "Automated checks and test fixtures",
            "Build and operations", "Deployment, CI, configuration, and developer tooling",
            "Shared libraries", "Reusable utilities and cross-cutting code");

    public RepositoryExplorerService(CodeChunkRepository chunks, SearchService search) {
        this.chunks = chunks;
        this.search = search;
    }

    public ExploreResponse explore(RepositoryRecord repository, String query) {
        List<CodeChunk> all = chunks.findByRepositoryId(repository.getId());
        Map<String, CodeChunk> files = all.stream().collect(Collectors.toMap(
                CodeChunk::getFile, Function.identity(),
                (a, b) -> a.getStartLine() <= b.getStartLine() ? a : b, LinkedHashMap::new));
        Map<String, Long> languageCounts = files.values().stream().collect(Collectors.groupingBy(
                c -> prettyLanguage(c.getLanguage()), Collectors.counting()));
        List<ExploreResponse.LanguageSummary> languages = languageCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8).map(e -> new ExploreResponse.LanguageSummary(e.getKey(), e.getValue().intValue())).toList();

        Map<String, List<String>> areas = new LinkedHashMap<>();
        for (String file : files.keySet()) {
            areas.computeIfAbsent(areaFor(file), ignored -> new ArrayList<>()).add(file);
        }
        List<ExploreResponse.AreaSummary> areaSummaries = areas.entrySet().stream()
                .sorted(Map.Entry.<String, List<String>>comparingByValue(Comparator.comparingInt(List::size)).reversed())
                .map(e -> new ExploreResponse.AreaSummary(e.getKey(), AREA_DESCRIPTIONS.get(e.getKey()), e.getValue().size(), e.getValue().stream().limit(4).toList()))
                .toList();

        List<ExploreResponse.EntryPoint> entryPoints = files.values().stream()
                .filter(c -> isEntryPoint(c.getFile()))
                .sorted(Comparator.comparingInt(c -> entryPointPriority(c.getFile())))
                .limit(8)
                .map(c -> new ExploreResponse.EntryPoint(c.getFile(), c.getStartLine(), sourceUrl(repository, c.getFile(), c.getStartLine(), c.getEndLine())))
                .toList();

        List<ExploreResponse.ExploreHit> results = query == null || query.isBlank() ? List.of()
                : search.search(repository.getId(), query, 16).stream()
                .map(c -> new ExploreResponse.ExploreHit(c.getFile(), prettyLanguage(c.getLanguage()), c.getStartLine(), c.getEndLine(),
                        c.getContent(), sourceUrl(repository, c.getFile(), c.getStartLine(), c.getEndLine()), explainMatch(c, query)))
                .toList();
        List<ExploreResponse.FlowStage> flowStages = flowStages(results);
        return new ExploreResponse(files.size(), all.size(), languages, areaSummaries, entryPoints, results, flowStages);
    }

    private List<ExploreResponse.FlowStage> flowStages(List<ExploreResponse.ExploreHit> results) {
        if (results.isEmpty()) return List.of();
        List<ExploreResponse.FlowStage> stages = new ArrayList<>();
        addStage(stages, results, "1. User interface", "Where a user action starts or visible state is rendered.",
                h -> matches(h, "frontend", "components", "pages", "screens", "views", "ui", "jsx", "tsx", "html"));
        addStage(stages, results, "2. API entry", "The route, controller, or handler that receives the request.",
                h -> matches(h, "controller", "routes", "route", "endpoint", "/api/", "handler", "resource"));
        addStage(stages, results, "3. Business logic", "The application service or use case that applies the behavior.",
                h -> matches(h, "service", "usecase", "use-case", "domain", "manager", "processor", "workflow"));
        addStage(stages, results, "4. Data boundary", "Where the feature reads or writes stored data.",
                h -> matches(h, "repository", "dao", "database", "entity", "model", "schema", "migration", "mapper"));
        List<ExploreResponse.ExploreHit> assigned = stages.stream().flatMap(s -> s.files().stream()).toList();
        List<ExploreResponse.ExploreHit> remaining = results.stream().filter(h -> !assigned.contains(h)).toList();
        if (!remaining.isEmpty()) stages.add(new ExploreResponse.FlowStage("5. Other related code", "Related implementation that does not match a common application layer.", remaining));
        return stages;
    }

    private void addStage(List<ExploreResponse.FlowStage> out, List<ExploreResponse.ExploreHit> hits, String name, String explanation,
                          java.util.function.Predicate<ExploreResponse.ExploreHit> predicate) {
        List<ExploreResponse.ExploreHit> matched = hits.stream().filter(predicate).toList();
        if (!matched.isEmpty()) out.add(new ExploreResponse.FlowStage(name, explanation, matched));
    }

    private boolean matches(ExploreResponse.ExploreHit hit, String... terms) {
        String path = hit.file().toLowerCase(Locale.ROOT);
        return Arrays.stream(terms).anyMatch(path::contains);
    }

    private String areaFor(String file) {
        String p = file.toLowerCase(Locale.ROOT);
        if (p.contains("test") || p.contains("spec")) return "Tests";
        if (p.contains("migration") || p.contains("schema") || p.contains("repository") || p.contains("dao") || p.contains("model") || p.contains("entity") || p.contains("database")) return "Data and storage";
        if (p.startsWith("frontend/") || p.contains("/components/") || p.contains("/pages/") || p.endsWith(".jsx") || p.endsWith(".tsx") || p.endsWith(".css")) return "User interface";
        if (p.contains("controller") || p.contains("/api/") || p.contains("/routes/") || p.contains("/service/")) return "API and services";
        if (p.startsWith(".github/") || p.contains("docker") || p.contains("terraform") || p.contains("deployment") || p.endsWith(".yml") || p.endsWith(".yaml") || p.endsWith(".gradle") || p.endsWith("pom.xml")) return "Build and operations";
        if (p.contains("util") || p.contains("common") || p.contains("shared")) return "Shared libraries";
        return "Product features";
    }

    private boolean isEntryPoint(String path) {
        String name = path.substring(path.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
        return Set.of("main.java", "main.py", "main.go", "main.rs", "main.jsx", "main.tsx", "app.py", "app.js", "app.jsx", "app.ts", "application.java", "application.yml", "server.js", "server.ts", "index.html", "manage.py").contains(name);
    }

    private int entryPointPriority(String path) {
        String name = path.substring(path.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
        return name.startsWith("main.") || name.startsWith("application.") ? 0 : 1;
    }

    private String prettyLanguage(String language) {
        if (language == null || language.isBlank()) return "Other";
        return switch (language.toLowerCase(Locale.ROOT)) {
            case "js" -> "JavaScript"; case "jsx" -> "React"; case "ts" -> "TypeScript"; case "tsx" -> "React + TypeScript";
            case "py" -> "Python"; case "java" -> "Java"; case "kt" -> "Kotlin"; case "yml", "yaml" -> "YAML";
            case "md" -> "Markdown"; case "html" -> "HTML"; case "scss" -> "SCSS"; default -> language.toUpperCase(Locale.ROOT);
        };
    }

    private String explainMatch(CodeChunk chunk, String query) {
        String path = chunk.getFile().toLowerCase(Locale.ROOT);
        String body = chunk.getContent().toLowerCase(Locale.ROOT);
        List<String> hits = Arrays.stream(query.toLowerCase(Locale.ROOT).split("[^a-z0-9_$]+"))
                .filter(t -> t.length() > 2 && (path.contains(t) || body.contains(t))).distinct().limit(3).toList();
        if (hits.isEmpty()) return "Related code found in this file";
        return "Matched " + String.join(", ", hits) + (path.contains(hits.getFirst()) ? " in the file path" : " in the code");
    }

    private String sourceUrl(RepositoryRecord repo, String file, int start, int end) {
        String branch = repo.getDefaultBranch() == null || repo.getDefaultBranch().isBlank() ? "main" : repo.getDefaultBranch();
        return "https://github.com/" + repo.getOwner() + "/" + repo.getName() + "/blob/" + branch + "/" + file + "#L" + start + "-L" + end;
    }
}
