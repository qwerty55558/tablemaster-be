package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.ChatRoom;
import com.mycompany.tablemaster.entity.ChatRoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    List<ChatRoom> findByStatus(ChatRoomStatus status);

    List<ChatRoom> findByStatusOrderByStartedAtDesc(ChatRoomStatus status);

    @Query("SELECT r FROM ChatRoom r WHERE r.status != 'CLOSED' ORDER BY r.startedAt DESC")
    List<ChatRoom> findAllNotClosedOrderByStartedAtDesc();
}
