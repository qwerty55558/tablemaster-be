package com.mycompany.tablemaster.controller;

import com.mycompany.tablemaster.dto.chat.*;
import com.mycompany.tablemaster.entity.ChatMessage;
import com.mycompany.tablemaster.entity.ChatRoom;
import com.mycompany.tablemaster.entity.ChatRoomParticipant;
import com.mycompany.tablemaster.entity.ChatRoomStatus;
import com.mycompany.tablemaster.security.UserAuthPrincipal;
import com.mycompany.tablemaster.service.ChatMessageService;
import com.mycompany.tablemaster.service.ChatReportService;
import com.mycompany.tablemaster.service.ChatRoomService;
import com.mycompany.tablemaster.service.WebSocketSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/staff/chat")
@RequiredArgsConstructor
@Slf4j
public class StaffChatController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final ChatReportService chatReportService;
    private final WebSocketSenderService webSocketSenderService;

    /**
     * 채팅방 목록 조회
     * GET /api/v1/staff/chat/rooms?search=A1&filter=REPORT
     */
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomListResponse>> getRooms(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String filter,
            @AuthenticationPrincipal UserAuthPrincipal principal) {

        List<ChatRoom> rooms;

        // 필터 적용
        if ("REPORT".equalsIgnoreCase(filter)) {
            rooms = chatRoomService.getActiveRooms().stream()
                    .filter(r -> r.getReportCount() > 0)
                    .toList();
        } else if ("GIFT".equalsIgnoreCase(filter)) {
            rooms = chatRoomService.getActiveRooms().stream()
                    .filter(r -> r.getGiftCount() > 0)
                    .toList();
        } else {
            rooms = chatRoomService.getActiveRooms();
        }

        Long userId = principal.getUserId();

        List<ChatRoomListResponse> response = rooms.stream()
                .map(room -> {
                    List<ChatRoomParticipant> participants = chatRoomService.getParticipants(room.getId());

                    // 검색 필터
                    if (search != null && !search.isBlank()) {
                        boolean matches = participants.stream()
                                .anyMatch(p -> p.getTableName().toLowerCase().contains(search.toLowerCase()));
                        if (!matches) return null;
                    }

                    long unreadCount = chatMessageService.getUnreadCount(room.getId(), userId);
                    return ChatRoomListResponse.from(room, participants, unreadCount);
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * 채팅방 상세 조회
     * GET /api/v1/staff/chat/rooms/{id}
     */
    @GetMapping("/rooms/{id}")
    public ResponseEntity<ChatRoomDetailResponse> getRoomDetail(@PathVariable Long id) {
        ChatRoom room = chatRoomService.getRoomDetail(id);
        List<ChatRoomParticipant> participants = chatRoomService.getParticipants(id);
        return ResponseEntity.ok(ChatRoomDetailResponse.from(room, participants));
    }

    /**
     * 메시지 히스토리 조회
     * GET /api/v1/staff/chat/rooms/{id}/messages?page=0&size=50
     */
    @GetMapping("/rooms/{id}/messages")
    public ResponseEntity<Page<ChatMessageResponse>> getMessages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ChatMessage> messages = chatMessageService.getMessages(id, pageable);
        Page<ChatMessageResponse> response = messages.map(ChatMessageResponse::from);
        return ResponseEntity.ok(response);
    }

    /**
     * 읽음 처리
     * POST /api/v1/staff/chat/rooms/{id}/read
     */
    @PostMapping("/rooms/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @RequestBody ChatReadRequest request,
            @AuthenticationPrincipal UserAuthPrincipal principal) {

        chatMessageService.updateReadPosition(id, principal.getUserId(), request.getLastMessageId());
        return ResponseEntity.ok().build();
    }

    /**
     * 제재
     * POST /api/v1/staff/chat/rooms/{id}/sanction
     */
    @PostMapping("/rooms/{id}/sanction")
    public ResponseEntity<Void> sanctionRoom(
            @PathVariable Long id,
            @AuthenticationPrincipal UserAuthPrincipal principal) {

        chatRoomService.sanctionRoom(id, principal.getUserId());
        return ResponseEntity.ok().build();
    }

    /**
     * 음소거 토글
     * POST /api/v1/staff/chat/rooms/{id}/mute
     */
    @PostMapping("/rooms/{id}/mute")
    public ResponseEntity<Map<String, Boolean>> toggleMute(
            @PathVariable Long id,
            @RequestBody ChatMuteRequest request) {

        boolean isMuted = chatRoomService.toggleMute(id, request.getDeviceId());
        return ResponseEntity.ok(Map.of("isMuted", isMuted));
    }

    /**
     * 알림 전송
     * POST /api/v1/staff/chat/notify/{deviceId}
     */
    @PostMapping("/notify/{deviceId}")
    public ResponseEntity<Void> notifyDevice(
            @PathVariable String deviceId,
            @RequestBody ChatNotifyRequest request) {

        webSocketSenderService.sendChatToDevice(deviceId, Map.of(
                "type", "STAFF_NOTIFICATION",
                "title", request.getTitle(),
                "message", request.getMessage()
        ));

        log.info("Staff notification sent to device: {}", deviceId);
        return ResponseEntity.ok().build();
    }
}
