package com.githubassistant.repository;
import com.githubassistant.model.CodeChunk;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
public interface CodeChunkRepository extends JpaRepository<CodeChunk,String> {
 List<CodeChunk> findByRepositoryId(String repositoryId);
 @Query(value="""
  WITH query_terms AS (SELECT to_tsquery('simple', :tsQuery) AS query)
  SELECT c.* FROM code_chunks c, query_terms q
  WHERE c.repository_id = :repositoryId
    AND to_tsvector('simple', coalesce(c.file, '') || ' ' || c.content) @@ q.query
  ORDER BY ts_rank_cd(to_tsvector('simple', coalesce(c.file, '') || ' ' || c.content), q.query) DESC
  LIMIT 300
  """, nativeQuery=true)
 List<CodeChunk> findKeywordCandidates(@Param("repositoryId") String repositoryId,@Param("tsQuery") String tsQuery);
 Optional<CodeChunk> findFirstByRepositoryIdAndFileAndStartLineAndEndLine(String repositoryId,String file,int startLine,int endLine);
 @Transactional
 void deleteByRepositoryId(String repositoryId);
}
