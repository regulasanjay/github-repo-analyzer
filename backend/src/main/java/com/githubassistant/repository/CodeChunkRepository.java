package com.githubassistant.repository;
import com.githubassistant.model.CodeChunk;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
public interface CodeChunkRepository extends JpaRepository<CodeChunk,String> {
 List<CodeChunk> findByRepositoryId(String repositoryId);
 Optional<CodeChunk> findFirstByRepositoryIdAndFileAndStartLineAndEndLine(String repositoryId,String file,int startLine,int endLine);
 @Transactional
 void deleteByRepositoryId(String repositoryId);
}
