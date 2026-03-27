package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.ChatMonitorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMonitorLogRepository extends JpaRepository<ChatMonitorLog, Long> {
}
