package com.githubassistant.repository;

import com.githubassistant.model.SavedLocation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface SavedLocationRepository extends JpaRepository<SavedLocation, String> {
    List<SavedLocation> findTop50ByRepositoryIdOrderByCreatedAtDesc(String repositoryId);
    Optional<SavedLocation> findFirstByRepositoryIdAndFileAndStartLineAndEndLine(String repositoryId, String file, int startLine, int endLine);
    @Transactional
    long deleteByIdAndRepositoryId(String id, String repositoryId);
}
