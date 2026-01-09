package com.mycompany.tablemaster.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.tablemaster.dto.notification.NotificationDTO;
import com.mycompany.tablemaster.entity.Notification;
import com.mycompany.tablemaster.entity.NotificationCategory;
import com.mycompany.tablemaster.exception.BusinessException;
import com.mycompany.tablemaster.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final WebSocketSenderService webSocketSenderService;
    private final ObjectMapper objectMapper;

    /**
     * 알림 생성 및 전송
     */
    @Transactional
    public Notification createAndSend(String deviceId, String title, String body,
                                       NotificationCategory category, Map<String, Object> data) {
        // 1. DB에 저장
        Notification notification = Notification.builder()
                .deviceId(deviceId)
                .title(title)
                .body(body)
                .category(category)
                .data(toJsonString(data))
                .build();

        notification = notificationRepository.save(notification);
        log.info("Notification saved: id={}, deviceId={}, title={}",
                 notification.getId(), deviceId, title);

        // 2. WebSocket 전송 시도
        NotificationDTO dto = NotificationDTO.from(notification);
        boolean delivered = webSocketSenderService.sendToDevice(deviceId, dto);

        // 3. 전송 상태 업데이트
        if (delivered) {
            notification.markAsDelivered();
            notificationRepository.save(notification);
            log.debug("Notification delivered via WebSocket: id={}", notification.getId());
        }

        return notification;
    }

    /**
     * 미전달 알림 재전송 (재접속 시 호출)
     */
    @Transactional
    public void sendUndeliveredNotifications(String deviceId) {
        List<Notification> undelivered = notificationRepository
                .findByDeviceIdAndIsDeliveredFalseOrderByCreatedAtAsc(deviceId);

        if (undelivered.isEmpty()) {
            return;
        }

        log.info("Sending {} undelivered notifications to device: {}",
                 undelivered.size(), deviceId);

        for (Notification notification : undelivered) {
            NotificationDTO dto = NotificationDTO.from(notification);
            boolean delivered = webSocketSenderService.sendToDevice(deviceId, dto);

            if (delivered) {
                notification.markAsDelivered();
                notificationRepository.save(notification);
            }
        }
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long notificationId, String deviceId) {
        Notification notification = notificationRepository
                .findByIdAndDeviceId(notificationId, deviceId)
                .orElseThrow(() -> new BusinessException(
                        "알림을 찾을 수 없습니다",
                        HttpStatus.NOT_FOUND,
                        "NOTIFICATION_001"
                ));

        notification.markAsRead();
        notificationRepository.save(notification);
    }

    /**
     * 모든 알림 읽음 처리
     */
    @Transactional
    public void markAllAsRead(String deviceId) {
        notificationRepository.markAllAsReadByDeviceId(deviceId);
    }

    /**
     * 알림 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getNotifications(String deviceId, Pageable pageable) {
        return notificationRepository
                .findByDeviceIdOrderByCreatedAtDesc(deviceId, pageable)
                .map(NotificationDTO::from);
    }

    /**
     * 읽지 않은 알림 개수
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(String deviceId) {
        return notificationRepository.countByDeviceIdAndIsReadFalse(deviceId);
    }

    private String toJsonString(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("Failed to serialize notification data", e);
            return null;
        }
    }
}
