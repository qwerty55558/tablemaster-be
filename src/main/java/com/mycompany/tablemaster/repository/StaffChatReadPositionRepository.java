package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.StaffChatReadPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StaffChatReadPositionRepository extends JpaRepository<StaffChatReadPosition, Long> {

    Optional<StaffChatReadPosition> findByUserIdAndChatRoomId(Long userId, Long chatRoomId);

    @Modifying
    @Query(value = "DELETE FROM staff_chat_read_positions WHERE chat_room_id = :chatRoomId", nativeQuery = true)
    void deleteByChatRoomId(@Param("chatRoomId") Long chatRoomId);
}
