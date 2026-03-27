package com.mycompany.tablemaster.controller;

import com.mycompany.tablemaster.dto.commerce.*;
import com.mycompany.tablemaster.service.CommerceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.mycompany.tablemaster.entity.BillStatus;

@RestController
@RequiredArgsConstructor
@Tag(name = "Commerce", description = "메뉴/선물/주문/정산 API")
public class CommerceController {

    private final CommerceService commerceService;

    @GetMapping("/api/v1/menu-items")
    @Operation(summary = "메뉴 목록 조회")
    public ResponseEntity<List<MenuItemResponse>> getMenuItems() {
        return ResponseEntity.ok(commerceService.getMenuItems());
    }

    @GetMapping("/api/v1/gifts")
    @Operation(summary = "선물 목록 조회")
    public ResponseEntity<List<GiftTypeResponse>> getGifts() {
        return ResponseEntity.ok(commerceService.getGiftTypes());
    }

    @GetMapping("/api/v1/tables/{identifier}/bill")
    @Operation(summary = "현재 bill 조회")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'DEVICE')")
    public ResponseEntity<BillResponse> getBill(@PathVariable String identifier) {
        return ResponseEntity.ok(commerceService.getCurrentBill(identifier));
    }

    @GetMapping("/api/v1/tables/{identifier}/bills")
    @Operation(summary = "테이블 bill 히스토리 조회")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'DEVICE')")
    public ResponseEntity<List<BillResponse>> getBillHistory(@PathVariable String identifier) {
        return ResponseEntity.ok(commerceService.getBillHistory(identifier));
    }

    @GetMapping("/api/v1/tables/{identifier}/orders")
    @Operation(summary = "현재 주문 조회")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'DEVICE')")
    public ResponseEntity<BillResponse> getOrders(@PathVariable String identifier) {
        return ResponseEntity.ok(commerceService.getCurrentBill(identifier));
    }

    @PostMapping("/api/v1/tables/{identifier}/orders")
    @Operation(summary = "주문 추가")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'DEVICE')")
    public ResponseEntity<BillResponse> createOrder(@PathVariable String identifier,
                                                    @Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(commerceService.createOrder(identifier, request));
    }

    @PatchMapping("/api/v1/tables/{identifier}/orders/{orderItemId}")
    @Operation(summary = "주문 수량 수정")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'DEVICE')")
    public ResponseEntity<BillResponse> updateOrderItem(@PathVariable String identifier,
                                                        @PathVariable Long orderItemId,
                                                        @Valid @RequestBody UpdateOrderItemRequest request) {
        return ResponseEntity.ok(commerceService.updateOrderItem(identifier, orderItemId, request));
    }

    @DeleteMapping("/api/v1/tables/{identifier}/orders/{orderItemId}")
    @Operation(summary = "주문 삭제")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'DEVICE')")
    public ResponseEntity<BillResponse> deleteOrderItem(@PathVariable String identifier,
                                                        @PathVariable Long orderItemId) {
        return ResponseEntity.ok(commerceService.deleteOrderItem(identifier, orderItemId));
    }

    @PostMapping("/api/v1/tables/{identifier}/bill/close")
    @Operation(summary = "bill 마감")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'DEVICE')")
    public ResponseEntity<BillResponse> closeBill(@PathVariable String identifier) {
        return ResponseEntity.ok(commerceService.closeBill(identifier));
    }

    @GetMapping("/api/v1/admin/bills")
    @Operation(summary = "전체 결제 내역 조회")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<List<BillResponse>> getAdminBills(@RequestParam(required = false) BillStatus status) {
        return ResponseEntity.ok(commerceService.getAdminBills(status));
    }

    @GetMapping("/api/v1/admin/bills/{billId}")
    @Operation(summary = "bill 상세 조회")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<BillResponse> getBillDetail(@PathVariable Long billId) {
        return ResponseEntity.ok(commerceService.getBillDetail(billId));
    }
}
