package com.mycompany.tablemaster.dto.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ForbiddenWordCreateRequest {
    @NotBlank
    private String word;
    private String reason;
}
