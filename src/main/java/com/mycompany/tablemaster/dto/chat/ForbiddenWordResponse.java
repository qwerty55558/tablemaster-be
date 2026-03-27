package com.mycompany.tablemaster.dto.chat;

import com.mycompany.tablemaster.entity.ForbiddenWord;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ForbiddenWordResponse {
    private Long id;
    private String word;
    private String reason;
    private Boolean isActive;
    private Long createdByUserId;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ForbiddenWordResponse from(ForbiddenWord forbiddenWord) {
        return ForbiddenWordResponse.builder()
                .id(forbiddenWord.getId())
                .word(forbiddenWord.getWord())
                .reason(forbiddenWord.getReason())
                .isActive(forbiddenWord.getIsActive())
                .createdByUserId(forbiddenWord.getCreatedByUserId())
                .createdByName(forbiddenWord.getCreatedByName())
                .createdAt(forbiddenWord.getCreatedAt())
                .updatedAt(forbiddenWord.getUpdatedAt())
                .build();
    }
}
