package com.mycompany.tablemaster.dto.commerce;

import com.mycompany.tablemaster.entity.MenuItem;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MenuItemResponse {
    private Long id;
    private String name;
    private Integer price;
    private String category;
    private String imageUrl;
    private Boolean isAvailable;

    public static MenuItemResponse from(MenuItem menuItem) {
        return MenuItemResponse.builder()
                .id(menuItem.getId())
                .name(menuItem.getName())
                .price(menuItem.getPrice())
                .category(menuItem.getCategory().name())
                .imageUrl(menuItem.getImageUrl())
                .isAvailable(menuItem.getIsAvailable())
                .build();
    }
}
