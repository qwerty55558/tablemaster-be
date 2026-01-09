package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 미전달 알림 조회 (재접속 시 사용)
     */
    List<Notification> findByDeviceIdAndIsDeliveredFalseOrderByCreatedAtAsc(String deviceId);

    /**
     * 디바이스별 알림 목록 (페이징)
     */
    Page<Notification> findByDeviceIdOrderByCreatedAtDesc(String deviceId, Pageable pageable);

    /**
     * 특정 알림 조회 (디바이스 검증 포함)
     */
    Optional<Notification> findByIdAndDeviceId(Long id, String deviceId);

    /**
     * 읽지 않은 알림 개수
     */
    long countByDeviceIdAndIsReadFalse(String deviceId);

    /**
     * 전체 읽음 처리
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.deviceId = :deviceId AND n.isRead = false")
    void markAllAsReadByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 오래된 알림 삭제 (스케줄러용)
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :before")
    void deleteOlderThan(@Param("before") LocalDateTime before);
}
