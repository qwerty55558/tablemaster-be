package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.ChatRoomHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRoomHistoryRepository extends JpaRepository<ChatRoomHistory, Long> {
}
