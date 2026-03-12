package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.ChatReport;
import com.mycompany.tablemaster.entity.ChatReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatReportRepository extends JpaRepository<ChatReport, Long> {

    List<ChatReport> findByChatRoomId(Long chatRoomId);

    List<ChatReport> findByStatus(ChatReportStatus status);

    void deleteByChatRoomId(Long chatRoomId);
}
