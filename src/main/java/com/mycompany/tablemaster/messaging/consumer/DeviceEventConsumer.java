package com.mycompany.tablemaster.messaging.consumer;

import com.mycompany.tablemaster.config.RabbitMQConfig;
import com.mycompany.tablemaster.event.DeviceEvent;
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
public class DeviceEventConsumer {

    private final WebSocketSenderService webSocketSenderService;

    @RabbitListener(queues = RabbitMQConfig.DEVICE_EVENT_QUEUE)
    public void handleDeviceEvent(DeviceEvent event, Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("Processing device event: deviceId={}, type={}", event.deviceId(), event.type());

            Map<String, Object> message = Map.of(
                    "type", event.type().name(),
                    "deviceId", event.deviceId(),
                    "timestamp", event.timestamp().toString()
            );

            switch (event.type()) {
                case DEVICE_REGISTRATION_EXPIRED -> {
                    webSocketSenderService.sendToAdmins(message);
                    log.info("Device registration expired notification sent to admins: {}", event.deviceId());
                }
            }

            channel.basicAck(deliveryTag, false);
            log.info("Device event processed successfully: deviceId={}, type={}", event.deviceId(), event.type());
        } catch (Exception e) {
            log.error("Failed to process device event: deviceId={}, type={}", event.deviceId(), event.type(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
