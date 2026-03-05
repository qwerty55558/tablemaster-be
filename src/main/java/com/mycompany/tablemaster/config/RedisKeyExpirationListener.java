package com.mycompany.tablemaster.config;

import com.mycompany.tablemaster.event.DeviceEvent;
import com.mycompany.tablemaster.messaging.producer.DeviceEventProducer;
import com.mycompany.tablemaster.service.TableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Redis Key 만료 이벤트 리스너
 * - device:pending:* 키 만료 시 RabbitMQ로 이벤트 발행
 * - device:disconnect:* 키 만료 시 테이블 DB 삭제 (TABLE_REMOVED)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisKeyExpirationListener implements MessageListener {

    private static final String PENDING_KEY_PREFIX = "device:pending:";
    private static final String DISCONNECT_KEY_PREFIX = "device:disconnect:";

    private final DeviceEventProducer deviceEventProducer;
    private final TableService tableService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();

        if (expiredKey.startsWith(PENDING_KEY_PREFIX)) {
            handlePendingExpired(expiredKey);
        } else if (expiredKey.startsWith(DISCONNECT_KEY_PREFIX)) {
            handleDisconnectExpired(expiredKey);
        }
    }

    private void handlePendingExpired(String expiredKey) {
        String deviceId = expiredKey.substring(PENDING_KEY_PREFIX.length());
        log.info("Device pending registration expired: {}", deviceId);
        deviceEventProducer.publish(DeviceEvent.registrationExpired(deviceId));
    }

    private void handleDisconnectExpired(String expiredKey) {
        String deviceId = expiredKey.substring(DISCONNECT_KEY_PREFIX.length());
        log.info("Device disconnect TTL expired, removing table: {}", deviceId);
        tableService.removeInactiveTable(deviceId);
    }
}
