package com.mycompany.tablemaster.messaging.consumer;

import com.mycompany.tablemaster.config.RabbitMQConfig;
import com.mycompany.tablemaster.event.DeviceEvent;
import com.mycompany.tablemaster.messaging.IdempotencyGuard;
import com.mycompany.tablemaster.messaging.ParkingLotPublisher;
import com.mycompany.tablemaster.messaging.RetryExecutor;
import com.mycompany.tablemaster.service.WebSocketSenderService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceEventConsumer {

    private static final String QUEUE = RabbitMQConfig.DEVICE_EVENT_QUEUE;

    private final WebSocketSenderService webSocketSenderService;
    private final IdempotencyGuard idempotencyGuard;
    private final RetryExecutor retryExecutor;
    private final ParkingLotPublisher parkingLotPublisher;

    @RabbitListener(queues = QUEUE)
    public void handleDeviceEvent(@Payload DeviceEvent event,
                                   Message rawMessage,
                                   Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        String idempotencyKey = idempotencyGuard.computeKey(QUEUE, rawMessage.getBody());

        if (idempotencyGuard.isAlreadyProcessed(idempotencyKey)) {
            log.info("Device duplicate skipped: deviceId={}, key={}", event.deviceId(), idempotencyKey);
            channel.basicAck(deliveryTag, false);
            return;
        }

        RetryExecutor.Outcome outcome = retryExecutor.execute(
                "device:" + event.deviceId(),
                () -> processDeviceEvent(event));

        if (outcome.succeeded()) {
            idempotencyGuard.markProcessed(idempotencyKey, QUEUE);
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            parkingLotPublisher.parkOrFallback(rawMessage, QUEUE, outcome.attempts(), outcome.failureSummary());
            idempotencyGuard.markProcessed(idempotencyKey, QUEUE);
            channel.basicAck(deliveryTag, false);
        } catch (Exception parkingFailed) {
            log.error("Device parking failed; requeue: deviceId={}", event.deviceId(), parkingFailed);
            channel.basicNack(deliveryTag, false, true);
        }
    }

    private void processDeviceEvent(DeviceEvent event) {
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
    }
}
