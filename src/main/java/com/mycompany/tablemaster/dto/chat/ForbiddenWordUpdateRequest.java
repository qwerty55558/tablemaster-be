package com.mycompany.tablemaster.dto.chat;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ForbiddenWordUpdateRequest {
    private String word;
    private String reason;
    private Boolean isActive;
}
