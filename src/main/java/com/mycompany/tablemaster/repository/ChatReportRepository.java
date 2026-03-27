package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.ChatReport;
import com.mycompany.tablemaster.entity.ChatReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatReportRepository extends JpaRepository<ChatReport, Long> {

    List<ChatReport> findByChatRoomId(Long chatRoomId);

    List<ChatReport> findByStatus(ChatReportStatus status);

    boolean existsByChatRoomIdAndReporterDeviceIdAndReportedDeviceIdAndStatus(
            Long chatRoomId,
            String reporterDeviceId,
            String reportedDeviceId,
            ChatReportStatus status
    );

    @Modifying
    @Query(value = "DELETE FROM chat_reports WHERE chat_room_id = :chatRoomId", nativeQuery = true)
    void deleteByChatRoomId(@Param("chatRoomId") Long chatRoomId);
}
