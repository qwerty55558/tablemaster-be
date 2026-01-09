package com.mycompany.tablemaster.messaging.consumer;

import com.mycompany.tablemaster.config.RabbitMQConfig;
import com.mycompany.tablemaster.event.TableEvent;
import com.mycompany.tablemaster.service.WebSocketSenderService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TableEventConsumer {

    private final WebSocketSenderService webSocketSenderService;

    @RabbitListener(queues = RabbitMQConfig.TABLE_RESET_QUEUE)
    public void handleTableReset(TableEvent event, Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("Processing table reset: tableId={}, deviceId={}",
                    event.tableId(), event.deviceId());

            // 해당 디바이스에 초기화 메시지 전송
            if (event.deviceId() != null) {
                Map<String, Object> message = Map.of(
                        "type", "table_reset",
                        "tableId", event.tableId(),
                        "timestamp", event.timestamp().toString()
                );

                boolean sent = webSocketSenderService.sendToDevice(event.deviceId(), message);
                if (sent) {
                    log.info("Table reset message sent to device: {}", event.deviceId());
                } else {
                    log.warn("Device not connected, table reset message not sent: {}", event.deviceId());
                }
            }

            // 테이블 목록 브로드캐스트
            webSocketSenderService.broadcast("table_reset", Map.of(
                    "tableId", event.tableId(),
                    "timestamp", event.timestamp().toString()
            ));

            channel.basicAck(deliveryTag, false);
            log.info("Table reset processed successfully: {}", event.tableId());
        } catch (Exception e) {
            log.error("Failed to process table reset: {}", event.tableId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
