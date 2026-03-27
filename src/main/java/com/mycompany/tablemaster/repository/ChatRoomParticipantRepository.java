package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.ChatRoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query("SELECT p FROM ChatRoomParticipant p JOIN FETCH p.chatRoom " +
           "WHERE p.deviceId = :deviceId")
    List<ChatRoomParticipant> findAllWithChatRoomByDeviceId(@Param("deviceId") String deviceId);

    @Query("SELECT COUNT(p1) > 0 FROM ChatRoomParticipant p1, ChatRoomParticipant p2 " +
           "WHERE p1.chatRoom = p2.chatRoom " +
           "AND p1.deviceId = :deviceId1 AND p2.deviceId = :deviceId2 " +
           "AND p1.chatRoom.status IN ('ACTIVE', 'SANCTIONED')")
    boolean existsActiveRoomBetween(@Param("deviceId1") String deviceId1, @Param("deviceId2") String deviceId2);

    @Query("SELECT COUNT(p) FROM ChatRoomParticipant p " +
           "WHERE p.deviceId = :deviceId AND p.chatRoom.status IN ('ACTIVE', 'SANCTIONED')")
    long countActiveOrSanctionedRoomsByDeviceId(@Param("deviceId") String deviceId);

    @Query("SELECT p FROM ChatRoomParticipant p JOIN FETCH p.chatRoom " +
           "WHERE p.deviceId IN :deviceIds AND p.chatRoom.status IN ('ACTIVE', 'SANCTIONED')")
    List<ChatRoomParticipant> findActiveOrSanctionedByDeviceIds(@Param("deviceIds") List<String> deviceIds);

    @Modifying
    @Query(value = "DELETE FROM chat_room_participants WHERE chat_room_id = :chatRoomId", nativeQuery = true)
    void deleteByChatRoomId(@Param("chatRoomId") Long chatRoomId);
}
