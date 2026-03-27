package com.mycompany.tablemaster.dto.commerce;

import com.mycompany.tablemaster.entity.GiftType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GiftTypeResponse {
    private Long id;
    private String code;
    private String displayName;
    private Integer price;
    private String imageUrl;
    private Boolean isAvailable;

    public static GiftTypeResponse from(GiftType giftType) {
        return GiftTypeResponse.builder()
                .id(giftType.getId())
                .code(giftType.getCode())
                .displayName(giftType.getDisplayName())
                .price(giftType.getPrice())
                .imageUrl(giftType.getImageUrl())
                .isAvailable(giftType.getIsAvailable())
                .build();
    }
}
