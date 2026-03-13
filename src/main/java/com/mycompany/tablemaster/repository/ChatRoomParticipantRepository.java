package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.ChatRoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomParticipantRepository extends JpaRepository<ChatRoomParticipant, Long> {

    List<ChatRoomParticipant> findByChatRoomId(Long chatRoomId);

    Optional<ChatRoomParticipant> findByChatRoomIdAndDeviceId(Long chatRoomId, String deviceId);

    List<ChatRoomParticipant> findByDeviceId(String deviceId);

    @Query("SELECT COUNT(p) FROM ChatRoomParticipant p " +
           "WHERE p.deviceId = :deviceId AND p.chatRoom.status = 'ACTIVE'")
    long countActiveRoomsByDeviceId(@Param("deviceId") String deviceId);

    @Query("SELECT p FROM ChatRoomParticipant p JOIN FETCH p.chatRoom " +
           "WHERE p.deviceId = :deviceId AND p.chatRoom.status = 'ACTIVE'")
    List<ChatRoomParticipant> findActiveByDeviceId(@Param("deviceId") String deviceId);

    void deleteByChatRoomId(Long chatRoomId);
}
