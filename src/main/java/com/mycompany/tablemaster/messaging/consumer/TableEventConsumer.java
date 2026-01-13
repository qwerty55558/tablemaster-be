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

    @RabbitListener(queues = RabbitMQConfig.TABLE_DELETED_QUEUE)
    public void handleTableDeleted(TableEvent event, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("Processing table deleted: tableId={}, deviceId={}",
                    event.tableId(), event.deviceId());

            // 해당 디바이스에 삭제 메시지 전송 → /queue/myTable
            if (event.deviceId() != null) {
                Map<String, Object> message = Map.of(
                        "type", "TABLE_DELETED",
                        "tableId", event.tableId(),
                        "timestamp", event.timestamp().toString()
                );

                boolean sent = webSocketSenderService.sendMyTableUpdate(event.deviceId(), message);
                if (sent) {
                    log.info("Table deleted message sent to device: {}", event.deviceId());
                } else {
                    log.warn("Device not connected, table deleted message not sent: {}", event.deviceId());
                }
            }

            channel.basicAck(deliveryTag, false);
            log.info("Table deleted processed successfully: {}", event.tableId());
        } catch (Exception e) {
            log.error("Failed to process table deleted: {}", event.tableId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
