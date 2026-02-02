package com.mycompany.tablemaster.config;

import com.mycompany.tablemaster.service.WebSocketSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Redis Key 만료 이벤트 리스너
 * - device:pending:* 키 만료 시 Admin과 해당 디바이스에 알림 전송
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisKeyExpirationListener implements MessageListener {

    private static final String PENDING_KEY_PREFIX = "device:pending:";

    private final WebSocketSenderService webSocketSenderService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();

        // device:pending:* 키만 처리
        if (!expiredKey.startsWith(PENDING_KEY_PREFIX)) {
            return;
        }

        String deviceId = expiredKey.substring(PENDING_KEY_PREFIX.length());
        log.info("Device pending registration expired: {}", deviceId);

        // Admin에게 만료 알림 전송
        webSocketSenderService.sendToAdmins(Map.of(
                "type", "DEVICE_REGISTRATION_EXPIRED",
                "deviceId", deviceId,
                "timestamp", Instant.now().toString()
        ));

        // TODO: 디바이스에게도 알림 (WebSocket 연결되어 있다면)
        // 현재 pending 상태 디바이스는 WebSocket 연결 전이므로 생략
    }
}
