package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.ChatModerationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatModerationHistoryRepository extends JpaRepository<ChatModerationHistory, Long> {
    List<ChatModerationHistory> findAllByOrderByProcessedAtDesc();
}
