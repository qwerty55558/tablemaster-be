package com.mycompany.tablemaster.messaging.consumer;

import com.mycompany.tablemaster.config.RabbitMQConfig;
import com.mycompany.tablemaster.entity.NotificationCategory;
import com.mycompany.tablemaster.event.NotificationEvent;
import com.mycompany.tablemaster.messaging.IdempotencyGuard;
import com.mycompany.tablemaster.messaging.ParkingLotPublisher;
import com.mycompany.tablemaster.messaging.RetryExecutor;
import com.mycompany.tablemaster.service.NotificationService;
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

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private static final String QUEUE = RabbitMQConfig.NOTIFICATION_INAPP_QUEUE;

    private final NotificationService notificationService;
    private final IdempotencyGuard idempotencyGuard;
    private final RetryExecutor retryExecutor;
    private final ParkingLotPublisher parkingLotPublisher;

    @RabbitListener(queues = QUEUE)
    public void handleInAppNotification(@Payload NotificationEvent event,
                                         Message rawMessage,
                                         Channel channel,
                                         @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        String idempotencyKey = idempotencyGuard.computeKey(QUEUE, rawMessage.getBody());

        if (idempotencyGuard.isAlreadyProcessed(idempotencyKey)) {
            log.info("Notification duplicate skipped: user={}, key={}", event.userId(), idempotencyKey);
            channel.basicAck(deliveryTag, false);
            return;
        }

        RetryExecutor.Outcome outcome = retryExecutor.execute(
                "notification:user=" + event.userId(),
                () -> processNotification(event));

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
            log.error("Notification parking failed; requeue: user={}", event.userId(), parkingFailed);
            channel.basicNack(deliveryTag, false, true);
        }
    }

    private void processNotification(NotificationEvent event) {
        log.info("Sending in-app notification: user={}, title={}", event.userId(), event.title());

        String deviceId = extractDeviceId(event);

        if (deviceId != null) {
            notificationService.createAndSend(
                    deviceId,
                    event.title(),
                    event.body(),
                    NotificationCategory.SYSTEM,
                    event.data()
            );
        } else {
            log.warn("No deviceId found in notification event, skipping: userId={}", event.userId());
        }
    }

    private String extractDeviceId(NotificationEvent event) {
        if (event.data() != null && event.data().containsKey("deviceId")) {
            return (String) event.data().get("deviceId");
        }
        return null;
    }
}
