package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.ChatRoom;
import com.mycompany.tablemaster.entity.ChatRoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    List<ChatRoom> findByStatus(ChatRoomStatus status);

    List<ChatRoom> findByStatusOrderByStartedAtDesc(ChatRoomStatus status);

    @Query("SELECT r FROM ChatRoom r WHERE r.status != 'CLOSED' ORDER BY r.startedAt DESC")
    List<ChatRoom> findAllNotClosedOrderByStartedAtDesc();

    @Query(value = """
            SELECT r.*
            FROM chat_rooms r
            WHERE r.status IN ('ACTIVE', 'SANCTIONED')
              AND EXISTS (
                  SELECT 1
                  FROM chat_room_participants p1
                  WHERE p1.chat_room_id = r.id
                    AND p1.device_id = :deviceId1
              )
              AND EXISTS (
                  SELECT 1
                  FROM chat_room_participants p2
                  WHERE p2.chat_room_id = r.id
                    AND p2.device_id = :deviceId2
              )
            ORDER BY
              CASE WHEN r.status = 'ACTIVE' THEN 0 ELSE 1 END,
              r.started_at DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<ChatRoom> findReusableRoomBetween(@Param("deviceId1") String deviceId1,
                                               @Param("deviceId2") String deviceId2);

    @Modifying
    @Query(value = "DELETE FROM chat_rooms WHERE id = :roomId", nativeQuery = true)
    void deleteByRoomId(@Param("roomId") Long roomId);

    @Modifying
    @Query("UPDATE ChatRoom r SET r.totalMessageCount = r.totalMessageCount + 1 WHERE r.id = :roomId")
    int incrementMessageCount(@Param("roomId") Long roomId);

    @Modifying
    @Query("UPDATE ChatRoom r SET r.giftCount = r.giftCount + 1 WHERE r.id = :roomId")
    int incrementGiftCount(@Param("roomId") Long roomId);

    @Modifying
    @Query("UPDATE ChatRoom r SET r.reportCount = r.reportCount + 1 WHERE r.id = :roomId")
    int incrementReportCount(@Param("roomId") Long roomId);
}
