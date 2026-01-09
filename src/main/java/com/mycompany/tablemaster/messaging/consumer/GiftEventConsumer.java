package com.mycompany.tablemaster.messaging.consumer;

import com.mycompany.tablemaster.config.RabbitMQConfig;
import com.mycompany.tablemaster.event.GiftEvent;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class GiftEventConsumer {

    @RabbitListener(queues = RabbitMQConfig.GIFT_PROCESS_QUEUE)
    public void handleGiftProcess(GiftEvent event, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("Processing gift: giftId={}, sender={}, receiver={}",
                    event.giftId(), event.senderId(), event.receiverId());

            // TODO: 선물 처리 로직 구현
            // 1. 재고 확인
            // 2. 결제 처리
            // 3. 선물 발송

            channel.basicAck(deliveryTag, false);
            log.info("Gift processed successfully: {}", event.giftId());
        } catch (Exception e) {
            log.error("Failed to process gift: {}", event.giftId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.GIFT_COMPLETE_QUEUE)
    public void handleGiftComplete(GiftEvent event, Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("Gift completed: giftId={}, receiver={}", event.giftId(), event.receiverId());

            // TODO: 선물 완료 알림 로직
            // 1. 수신자에게 알림 전송
            // 2. 발신자에게 전달 완료 알림

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to handle gift completion: {}", event.giftId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
