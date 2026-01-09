package com.mycompany.tablemaster.controller;

import com.mycompany.tablemaster.event.*;
import com.mycompany.tablemaster.messaging.producer.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/test/messages")
@RequiredArgsConstructor
@Tag(name = "Message Test", description = "RabbitMQ 메시지 테스트 API (개발용)")
public class MessageTestController {

    private final GiftEventProducer giftEventProducer;
    private final ChatEventProducer chatEventProducer;
    private final NotificationEventProducer notificationEventProducer;

    @PostMapping("/gift")
    @Operation(summary = "선물 이벤트 테스트", description = "선물 처리 큐에 메시지 발행")
    public ResponseEntity<Map<String, String>> testGift(
            @RequestParam(defaultValue = "1") Long giftId,
            @RequestParam(defaultValue = "100") Long senderId,
            @RequestParam(defaultValue = "200") Long receiverId,
            @RequestParam(defaultValue = "테스트 케이크") String productName) {

        GiftEvent event = GiftEvent.created(giftId, senderId, receiverId, productName);
        giftEventProducer.sendGiftProcess(event);

        return ResponseEntity.ok(Map.of(
                "status", "sent",
                "queue", "gift.process.queue",
                "event", event.toString()
        ));
    }

    @PostMapping("/gift/complete")
    @Operation(summary = "선물 완료 이벤트 테스트", description = "선물 완료 큐에 메시지 발행")
    public ResponseEntity<Map<String, String>> testGiftComplete(
            @RequestParam(defaultValue = "1") Long giftId,
            @RequestParam(defaultValue = "100") Long senderId,
            @RequestParam(defaultValue = "200") Long receiverId,
            @RequestParam(defaultValue = "테스트 케이크") String productName) {

        GiftEvent event = GiftEvent.completed(giftId, senderId, receiverId, productName);
        giftEventProducer.sendGiftComplete(event);

        return ResponseEntity.ok(Map.of(
                "status", "sent",
                "queue", "gift.complete.queue",
                "event", event.toString()
        ));
    }

    @PostMapping("/chat")
    @Operation(summary = "채팅 메시지 테스트", description = "채팅 큐에 메시지 발행")
    public ResponseEntity<Map<String, String>> testChat(
            @RequestParam(defaultValue = "1") Long roomId,
            @RequestParam(defaultValue = "100") Long senderId,
            @RequestParam(defaultValue = "안녕하세요!") String message) {

        ChatEvent event = ChatEvent.message(roomId, senderId, message);
        chatEventProducer.sendMessage(event);

        return ResponseEntity.ok(Map.of(
                "status", "sent",
                "queue", "chat.message.queue",
                "routingKey", "chat.message." + roomId,
                "event", event.toString()
        ));
    }

    @PostMapping("/chat/join")
    @Operation(summary = "채팅방 입장 테스트", description = "채팅 입장 이벤트 발행")
    public ResponseEntity<Map<String, String>> testChatJoin(
            @RequestParam(defaultValue = "1") Long roomId,
            @RequestParam(defaultValue = "100") Long userId) {

        ChatEvent event = ChatEvent.join(roomId, userId);
        chatEventProducer.sendJoin(event);

        return ResponseEntity.ok(Map.of(
                "status", "sent",
                "queue", "chat.message.queue",
                "event", event.toString()
        ));
    }

    @PostMapping("/notification/push")
    @Operation(summary = "푸시 알림 테스트", description = "푸시 알림 큐에 메시지 발행 (Fanout)")
    public ResponseEntity<Map<String, String>> testPushNotification(
            @RequestParam(defaultValue = "100") Long userId,
            @RequestParam(defaultValue = "테스트 알림") String title,
            @RequestParam(defaultValue = "이것은 테스트 푸시 알림입니다.") String body) {

        NotificationEvent event = NotificationEvent.push(userId, title, body);
        notificationEventProducer.sendNotification(event);

        return ResponseEntity.ok(Map.of(
                "status", "sent",
                "queues", "notification.push.queue, notification.inapp.queue (fanout)",
                "event", event.toString()
        ));
    }

    @PostMapping("/notification/inapp")
    @Operation(summary = "인앱 알림 테스트", description = "인앱 알림 큐에 메시지 발행 (Fanout)")
    public ResponseEntity<Map<String, String>> testInAppNotification(
            @RequestParam(defaultValue = "100") Long userId,
            @RequestParam(defaultValue = "새 메시지") String title,
            @RequestParam(defaultValue = "친구가 메시지를 보냈습니다.") String body) {

        NotificationEvent event = NotificationEvent.inApp(userId, title, body);
        notificationEventProducer.sendNotification(event);

        return ResponseEntity.ok(Map.of(
                "status", "sent",
                "queues", "notification.push.queue, notification.inapp.queue (fanout)",
                "event", event.toString()
        ));
    }
}
