package com.githubassistant.service;

import com.githubassistant.dto.SaveLocationRequest;
import com.githubassistant.model.CodeChunk;
import com.githubassistant.model.RepositoryRecord;
import com.githubassistant.model.SavedLocation;
import com.githubassistant.repository.CodeChunkRepository;
import com.githubassistant.repository.RepositoryRecordRepository;
import com.githubassistant.repository.SavedLocationRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SavedLocationService {
    private final SavedLocationRepository saved;
    private final CodeChunkRepository chunks;
    private final RepositoryRecordRepository repositories;

    public SavedLocationService(SavedLocationRepository saved, CodeChunkRepository chunks, RepositoryRecordRepository repositories) {
        this.saved=saved; this.chunks=chunks; this.repositories=repositories;
    }

    public List<SavedLocation> list(String repositoryId) { return saved.findTop50ByRepositoryIdOrderByCreatedAtDesc(repositoryId); }

    public SavedLocation save(String repositoryId, SaveLocationRequest request) {
        if (request.startLine() > request.endLine()) throw new IllegalArgumentException("The start line must be before the end line.");
        return saved.findFirstByRepositoryIdAndFileAndStartLineAndEndLine(repositoryId, request.file(), request.startLine(), request.endLine())
                .orElseGet(() -> create(repositoryId, request));
    }

    private SavedLocation create(String repositoryId, SaveLocationRequest request) {
        CodeChunk chunk = chunks.findFirstByRepositoryIdAndFileAndStartLineAndEndLine(repositoryId, request.file(), request.startLine(), request.endLine())
                .orElseThrow(() -> new IllegalArgumentException("That code location is no longer in the indexed repository."));
        RepositoryRecord repository = repositories.findById(repositoryId).orElseThrow();
        String branch = repository.getDefaultBranch() == null || repository.getDefaultBranch().isBlank() ? "main" : repository.getDefaultBranch();
        String url = "https://github.com/" + repository.getOwner() + "/" + repository.getName() + "/blob/" + branch + "/" + chunk.getFile() + "#L" + chunk.getStartLine() + "-L" + chunk.getEndLine();
        return saved.save(new SavedLocation(repositoryId, chunk.getFile(), chunk.getStartLine(), chunk.getEndLine(), chunk.getContent(), url, request.note()));
    }

    public boolean remove(String repositoryId, String locationId) { return saved.deleteByIdAndRepositoryId(locationId, repositoryId) > 0; }
}
