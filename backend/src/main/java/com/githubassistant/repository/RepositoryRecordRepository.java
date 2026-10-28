package com.githubassistant.repository;
import com.githubassistant.model.RepositoryRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RepositoryRecordRepository extends JpaRepository<RepositoryRecord,String> {
    Optional<RepositoryRecord> findFirstByUrlOrderByCreatedAtDesc(String url);
}
