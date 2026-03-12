package com.mycompany.tablemaster.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "staff_chat_read_positions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "chat_room_id"}))
@Getter
@Setter
@NoArgsConstructor
public class StaffChatReadPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    @Column(name = "last_read_message_id", nullable = false)
    private Long lastReadMessageId = 0L;

    @Builder
    public StaffChatReadPosition(Long userId, Long chatRoomId, Long lastReadMessageId) {
        this.userId = userId;
        this.chatRoomId = chatRoomId;
        this.lastReadMessageId = lastReadMessageId != null ? lastReadMessageId : 0L;
    }
}
