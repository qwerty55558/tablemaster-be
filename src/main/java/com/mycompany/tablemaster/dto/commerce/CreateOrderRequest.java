package com.mycompany.tablemaster.dto.commerce;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class CreateOrderRequest {
    @Valid
    @NotEmpty
    private List<OrderItemRequest> items;
}
