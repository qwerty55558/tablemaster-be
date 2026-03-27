package com.mycompany.tablemaster.controller;

import com.mycompany.tablemaster.dto.chat.ChatReportRequest;
import com.mycompany.tablemaster.dto.chat.ChatReportResponse;
import com.mycompany.tablemaster.security.DeviceAuthPrincipal;
import com.mycompany.tablemaster.service.ChatReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/device/chat")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DEVICE')")
public class DeviceChatReportController {

    private final ChatReportService chatReportService;

    @PostMapping("/rooms/{roomId}/reports")
    public ResponseEntity<ChatReportResponse> createReport(
            @PathVariable Long roomId,
            @Valid @RequestBody ChatReportRequest request,
            @AuthenticationPrincipal DeviceAuthPrincipal principal
    ) {
        return ResponseEntity.ok(ChatReportResponse.from(
                chatReportService.createReport(
                        roomId,
                        principal.getDeviceId(),
                        request.getReportedDeviceId(),
                        request.getReason()
                )
        ));
    }
}
