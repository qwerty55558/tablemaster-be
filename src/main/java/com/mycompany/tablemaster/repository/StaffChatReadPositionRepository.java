package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.StaffChatReadPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StaffChatReadPositionRepository extends JpaRepository<StaffChatReadPosition, Long> {

    Optional<StaffChatReadPosition> findByUserIdAndChatRoomId(Long userId, Long chatRoomId);

    void deleteByChatRoomId(Long chatRoomId);
}
