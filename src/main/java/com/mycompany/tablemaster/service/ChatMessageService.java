package com.mycompany.tablemaster.service;

import com.mycompany.tablemaster.entity.*;
import com.mycompany.tablemaster.event.ChatEvent;
import com.mycompany.tablemaster.repository.ChatMessageRepository;
import com.mycompany.tablemaster.repository.StaffChatReadPositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final StaffChatReadPositionRepository staffChatReadPositionRepository;

    @Transactional
    public ChatMessage saveMessage(ChatRoom chatRoom, ChatEvent event) {
        ChatMessageType messageType = resolveMessageType(event);

        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .senderDeviceId(event.senderDeviceId())
                .senderTableName(event.senderTableName())
                .content(event.message())
                .type(messageType)
                .build();

        ChatMessage saved = chatMessageRepository.save(message);

        chatRoom.incrementMessageCount();
        if (messageType == ChatMessageType.GIFT) {
            chatRoom.incrementGiftCount();
        }

        log.info("Chat message saved: roomId={}, messageId={}, type={}", chatRoom.getId(), saved.getId(), messageType);
        return saved;
    }

    @Transactional
    public ChatMessage saveSystemMessage(ChatRoom chatRoom, String content) {
        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .content(content)
                .type(ChatMessageType.SYSTEM)
                .build();

        ChatMessage saved = chatMessageRepository.save(message);
        chatRoom.incrementMessageCount();
        log.info("System message saved: roomId={}, messageId={}", chatRoom.getId(), saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<ChatMessage> getMessages(Long roomId, Pageable pageable) {
        return chatMessageRepository.findByChatRoomIdOrderByCreatedAtDesc(roomId, pageable);
    }

    @Transactional
    public void updateReadPosition(Long roomId, Long userId, Long lastMessageId) {
        StaffChatReadPosition position = staffChatReadPositionRepository
                .findByUserIdAndChatRoomId(userId, roomId)
                .orElseGet(() -> StaffChatReadPosition.builder()
                        .userId(userId)
                        .chatRoomId(roomId)
                        .lastReadMessageId(0L)
                        .build());

        if (lastMessageId > position.getLastReadMessageId()) {
            position.setLastReadMessageId(lastMessageId);
            staffChatReadPositionRepository.save(position);
            log.debug("Read position updated: roomId={}, userId={}, lastMessageId={}", roomId, userId, lastMessageId);
        }
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long roomId, Long userId) {
        StaffChatReadPosition position = staffChatReadPositionRepository
                .findByUserIdAndChatRoomId(userId, roomId)
                .orElse(null);

        Long lastReadId = (position != null) ? position.getLastReadMessageId() : 0L;
        return chatMessageRepository.countByChatRoomIdAndIdGreaterThan(roomId, lastReadId);
    }

    private ChatMessageType resolveMessageType(ChatEvent event) {
        if (event.messageType() != null) {
            return switch (event.messageType()) {
                case "GIFT" -> ChatMessageType.GIFT;
                case "SYSTEM" -> ChatMessageType.SYSTEM;
                default -> ChatMessageType.MESSAGE;
            };
        }
        return switch (event.type()) {
            case JOIN -> ChatMessageType.JOIN;
            case LEAVE -> ChatMessageType.LEAVE;
            case GIFT -> ChatMessageType.GIFT;
            default -> ChatMessageType.MESSAGE;
        };
    }
}
