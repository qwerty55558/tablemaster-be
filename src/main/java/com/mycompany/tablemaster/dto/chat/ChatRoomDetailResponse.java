package com.mycompany.tablemaster.dto.chat;

import com.mycompany.tablemaster.entity.ChatRoom;
import com.mycompany.tablemaster.entity.ChatRoomParticipant;
import com.mycompany.tablemaster.entity.ChatRoomStatus;
import com.mycompany.tablemaster.entity.SanctionType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ChatRoomDetailResponse {

    private Long id;
    private ChatRoomStatus status;
    private SanctionType sanctionType;
    private String sanctionReason;
    private LocalDateTime sanctionExpiresAt;
    private LocalDateTime startedAt;
    private LocalDateTime closedAt;
    private Integer totalMessageCount;
    private Integer giftCount;
    private Integer reportCount;
    private List<ChatRoomListResponse.ParticipantInfo> participants;

    public static ChatRoomDetailResponse from(ChatRoom room, List<ChatRoomParticipant> participants) {
        return ChatRoomDetailResponse.builder()
                .id(room.getId())
                .status(room.getStatus())
                .sanctionType(room.getSanctionType())
                .sanctionReason(room.getSanctionReason())
                .sanctionExpiresAt(room.getSanctionExpiresAt())
                .startedAt(room.getStartedAt())
                .closedAt(room.getClosedAt())
                .totalMessageCount(room.getTotalMessageCount())
                .giftCount(room.getGiftCount())
                .reportCount(room.getReportCount())
                .participants(participants.stream().map(ChatRoomListResponse.ParticipantInfo::from).toList())
                .build();
    }
}
