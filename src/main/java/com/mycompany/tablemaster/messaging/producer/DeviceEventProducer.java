package com.mycompany.tablemaster.messaging.producer;

import com.mycompany.tablemaster.config.RabbitMQConfig;
import com.mycompany.tablemaster.event.DeviceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public void publish(DeviceEvent event) {
        log.info("Publishing device event: deviceId={}, type={}", event.deviceId(), event.type());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DEVICE_EXCHANGE,
                RabbitMQConfig.DEVICE_EVENT_KEY,
                event
        );
    }
}
