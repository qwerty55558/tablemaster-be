package com.mycompany.tablemaster.messaging.producer;

import com.mycompany.tablemaster.config.RabbitMQConfig;
import com.mycompany.tablemaster.event.GiftEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GiftEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendGiftProcess(GiftEvent event) {
        log.info("Sending gift process event: {}", event);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.GIFT_EXCHANGE,
                RabbitMQConfig.GIFT_PROCESS_KEY,
                event
        );
    }

    public void sendGiftComplete(GiftEvent event) {
        log.info("Sending gift complete event: {}", event);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.GIFT_EXCHANGE,
                RabbitMQConfig.GIFT_COMPLETE_KEY,
                event
        );
    }
}
