package com.mycompany.tablemaster.messaging.producer;

import com.mycompany.tablemaster.config.RabbitMQConfig;
import com.mycompany.tablemaster.event.TableEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TableEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendTableDeleted(TableEvent event) {
        log.info("Sending table deleted event: tableId={}, deviceId={}", event.tableId(), event.deviceId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TABLE_EXCHANGE,
                RabbitMQConfig.TABLE_DELETED_KEY,
                event
        );
    }
}
