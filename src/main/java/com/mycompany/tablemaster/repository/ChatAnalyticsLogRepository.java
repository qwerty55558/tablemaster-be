package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.ChatAnalyticsLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatAnalyticsLogRepository extends JpaRepository<ChatAnalyticsLog, Long> {
    List<ChatAnalyticsLog> findByLoggedAtBetweenOrderByLoggedAtAsc(LocalDateTime from, LocalDateTime to);
}
