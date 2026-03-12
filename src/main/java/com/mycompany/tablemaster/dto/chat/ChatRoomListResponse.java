package com.mycompany.tablemaster.dto.chat;

import com.mycompany.tablemaster.entity.ChatRoom;
import com.mycompany.tablemaster.entity.ChatRoomParticipant;
import com.mycompany.tablemaster.entity.ChatRoomStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ChatRoomListResponse {

    private Long id;
    private ChatRoomStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime closedAt;
    private Integer totalMessageCount;
    private Integer giftCount;
    private Integer reportCount;
    private List<ParticipantInfo> participants;
    private Long unreadCount;

    @Getter
    @Builder
    public static class ParticipantInfo {
        private String deviceId;
        private String tableName;
        private Boolean isMuted;

        public static ParticipantInfo from(ChatRoomParticipant participant) {
            return ParticipantInfo.builder()
                    .deviceId(participant.getDeviceId())
                    .tableName(participant.getTableName())
                    .isMuted(participant.getIsMuted())
                    .build();
        }
    }

    public static ChatRoomListResponse from(ChatRoom room, List<ChatRoomParticipant> participants, Long unreadCount) {
        return ChatRoomListResponse.builder()
                .id(room.getId())
                .status(room.getStatus())
                .startedAt(room.getStartedAt())
                .closedAt(room.getClosedAt())
                .totalMessageCount(room.getTotalMessageCount())
                .giftCount(room.getGiftCount())
                .reportCount(room.getReportCount())
                .participants(participants.stream().map(ParticipantInfo::from).toList())
                .unreadCount(unreadCount)
                .build();
    }
}
