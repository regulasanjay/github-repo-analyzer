package com.githubassistant.repository;
import com.githubassistant.model.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ChatMessageRepository extends JpaRepository<ChatMessage,String> { List<ChatMessage> findTop8ByRepositoryIdOrderByCreatedAtDesc(String repositoryId); List<ChatMessage> findTop20ByRepositoryIdOrderByCreatedAtDesc(String repositoryId); }
