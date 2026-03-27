package com.mycompany.tablemaster.dto.commerce;

import com.mycompany.tablemaster.entity.OrderItem;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrderItemResponse {
    private Long id;
    private Long menuItemId;
    private String name;
    private Integer price;
    private Integer quantity;
    private String category;
    private String imageUrl;
    private LocalDateTime createdAt;

    public static OrderItemResponse from(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .menuItemId(item.getMenuItem().getId())
                .name(item.getName())
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .category(item.getCategory().name())
                .imageUrl(item.getMenuItem().getImageUrl())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
