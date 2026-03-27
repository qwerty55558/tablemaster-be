package com.mycompany.tablemaster.dto.commerce;

import com.mycompany.tablemaster.entity.Bill;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class BillResponse {
    private Long id;
    private String deviceId;
    private String tableName;
    private String status;
    private Long totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;
    private List<OrderItemResponse> orderItems;
    private List<GiftOrderResponse> giftOrders;

    public static BillResponse from(Bill bill) {
        return BillResponse.builder()
                .id(bill.getId())
                .deviceId(bill.getDeviceId())
                .tableName(bill.getTableName())
                .status(bill.getStatus().name())
                .totalAmount(bill.getTotalAmount())
                .createdAt(bill.getCreatedAt())
                .updatedAt(bill.getUpdatedAt())
                .closedAt(bill.getClosedAt())
                .orderItems(bill.getOrderItems().stream().map(OrderItemResponse::from).toList())
                .giftOrders(bill.getGiftOrders().stream().map(GiftOrderResponse::from).toList())
                .build();
    }
}
