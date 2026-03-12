package com.mycompany.tablemaster.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chat_room_participants")
@Getter
@Setter
@NoArgsConstructor
public class ChatRoomParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "table_name", nullable = false)
    private String tableName;

    @Column(name = "is_muted", nullable = false)
    private Boolean isMuted = false;

    @Builder
    public ChatRoomParticipant(ChatRoom chatRoom, String deviceId, String tableName) {
        this.chatRoom = chatRoom;
        this.deviceId = deviceId;
        this.tableName = tableName;
        this.isMuted = false;
    }
}
