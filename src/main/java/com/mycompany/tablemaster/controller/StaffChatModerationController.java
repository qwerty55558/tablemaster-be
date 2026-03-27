package com.mycompany.tablemaster.controller;

import com.mycompany.tablemaster.dto.chat.*;
import com.mycompany.tablemaster.security.UserAuthPrincipal;
import com.mycompany.tablemaster.service.ChatModerationHistoryService;
import com.mycompany.tablemaster.service.ChatReportService;
import com.mycompany.tablemaster.service.ForbiddenWordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/staff/chat")
@RequiredArgsConstructor
public class StaffChatModerationController {

    private final ChatModerationHistoryService chatModerationHistoryService;
    private final ChatReportService chatReportService;
    private final ForbiddenWordService forbiddenWordService;

    @GetMapping("/moderation/histories")
    public ResponseEntity<List<ChatModerationHistoryListResponse>> getModerationHistories() {
        return ResponseEntity.ok(chatModerationHistoryService.getModerationHistories());
    }

    @GetMapping("/moderation/histories/{id}")
    public ResponseEntity<ChatModerationHistoryDetailResponse> getModerationHistory(@PathVariable Long id) {
        return ResponseEntity.ok(chatModerationHistoryService.getModerationHistory(id));
    }

    @GetMapping("/reports/pending")
    public ResponseEntity<List<ChatReportResponse>> getPendingReports() {
        return ResponseEntity.ok(chatReportService.getPendingReports().stream()
                .map(ChatReportResponse::from)
                .toList());
    }

    @GetMapping("/rooms/{roomId}/reports")
    public ResponseEntity<List<ChatReportResponse>> getRoomReports(@PathVariable Long roomId) {
        return ResponseEntity.ok(chatReportService.getReportsByRoom(roomId).stream()
                .map(ChatReportResponse::from)
                .toList());
    }

    @PostMapping("/reports/{reportId}/review")
    public ResponseEntity<ChatReportResponse> reviewReport(@PathVariable Long reportId,
                                                           @Valid @RequestBody ChatReportReviewRequest request,
                                                           @AuthenticationPrincipal UserAuthPrincipal principal) {
        return ResponseEntity.ok(ChatReportResponse.from(
                chatReportService.reviewReport(reportId, principal.getUserId(), request.getStatus())
        ));
    }

    @GetMapping("/forbidden-words")
    public ResponseEntity<List<ForbiddenWordResponse>> getForbiddenWords() {
        return ResponseEntity.ok(forbiddenWordService.getForbiddenWords());
    }

    @PostMapping("/forbidden-words")
    public ResponseEntity<ForbiddenWordResponse> createForbiddenWord(@Valid @RequestBody ForbiddenWordCreateRequest request,
                                                                     @AuthenticationPrincipal UserAuthPrincipal principal) {
        return ResponseEntity.ok(forbiddenWordService.createForbiddenWord(request, principal.getUserId()));
    }

    @PutMapping("/forbidden-words/{id}")
    public ResponseEntity<ForbiddenWordResponse> updateForbiddenWord(@PathVariable Long id,
                                                                     @RequestBody ForbiddenWordUpdateRequest request) {
        return ResponseEntity.ok(forbiddenWordService.updateForbiddenWord(id, request));
    }

    @DeleteMapping("/forbidden-words/{id}")
    public ResponseEntity<Void> deleteForbiddenWord(@PathVariable Long id) {
        forbiddenWordService.deleteForbiddenWord(id);
        return ResponseEntity.ok().build();
    }
}
