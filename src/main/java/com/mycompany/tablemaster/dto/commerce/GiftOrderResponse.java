package com.mycompany.tablemaster.dto.commerce;

import com.mycompany.tablemaster.entity.GiftOrder;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GiftOrderResponse {
    private Long id;
    private Long giftTypeId;
    private String code;
    private String displayName;
    private Integer price;
    private Integer quantity;
    private String imageUrl;
    private Long chatRoomId;
    private LocalDateTime createdAt;

    public static GiftOrderResponse from(GiftOrder giftOrder) {
        return GiftOrderResponse.builder()
                .id(giftOrder.getId())
                .giftTypeId(giftOrder.getGiftType().getId())
                .code(giftOrder.getCode())
                .displayName(giftOrder.getDisplayName())
                .price(giftOrder.getPrice())
                .quantity(giftOrder.getQuantity())
                .imageUrl(giftOrder.getGiftType().getImageUrl())
                .chatRoomId(giftOrder.getChatRoomId())
                .createdAt(giftOrder.getCreatedAt())
                .build();
    }
}
