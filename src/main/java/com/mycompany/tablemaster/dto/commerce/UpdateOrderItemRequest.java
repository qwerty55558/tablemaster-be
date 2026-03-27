package com.mycompany.tablemaster.dto.commerce;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateOrderItemRequest {
    @NotNull
    @Min(1)
    private Integer quantity;
}
