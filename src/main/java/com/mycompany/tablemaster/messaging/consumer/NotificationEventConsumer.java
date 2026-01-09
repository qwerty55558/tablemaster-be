package com.mycompany.tablemaster.messaging.consumer;

import com.mycompany.tablemaster.config.RabbitMQConfig;
import com.mycompany.tablemaster.event.NotificationEvent;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class NotificationEventConsumer {

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_PUSH_QUEUE)
    public void handlePushNotification(NotificationEvent event, Channel channel,
                                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("Sending push notification: user={}, title={}", event.userId(), event.title());

            // TODO: 푸시 알림 전송 로직
            // 1. FCM/APNs 토큰 조회
            // 2. 푸시 메시지 전송

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to send push notification to user: {}", event.userId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_INAPP_QUEUE)
    public void handleInAppNotification(NotificationEvent event, Channel channel,
                                         @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("Sending in-app notification: user={}, title={}", event.userId(), event.title());

            // TODO: 인앱 알림 처리 로직
            // 1. 알림 DB 저장
            // 2. WebSocket으로 실시간 전달

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to send in-app notification to user: {}", event.userId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
