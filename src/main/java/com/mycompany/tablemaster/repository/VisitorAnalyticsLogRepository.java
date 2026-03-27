package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.VisitorAnalyticsLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VisitorAnalyticsLogRepository extends JpaRepository<VisitorAnalyticsLog, Long> {
    List<VisitorAnalyticsLog> findByLoggedAtBetweenOrderByLoggedAtAsc(LocalDateTime from, LocalDateTime to);
}
