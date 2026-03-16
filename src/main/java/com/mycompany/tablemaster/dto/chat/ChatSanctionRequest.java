package com.mycompany.tablemaster.dto.chat;

import com.mycompany.tablemaster.entity.SanctionType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatSanctionRequest {
    @NotNull
    private SanctionType type;
    private String reason;
    private Integer durationMinutes;
}
