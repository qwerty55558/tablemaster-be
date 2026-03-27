package com.mycompany.tablemaster.service;

import com.mycompany.tablemaster.dto.commerce.*;
import com.mycompany.tablemaster.entity.*;
import com.mycompany.tablemaster.exception.BusinessException;
import com.mycompany.tablemaster.repository.*;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommerceService {

    private final MenuItemRepository menuItemRepository;
    private final GiftTypeRepository giftTypeRepository;
    private final BillRepository billRepository;
    private final OrderItemRepository orderItemRepository;
    private final GiftOrderRepository giftOrderRepository;
    private final TableRepository tableRepository;

    public List<MenuItemResponse> getMenuItems() {
        return menuItemRepository.findByIsAvailableTrueOrderByCategoryAscNameAsc().stream()
                .map(MenuItemResponse::from)
                .toList();
    }

    public List<GiftTypeResponse> getGiftTypes() {
        return giftTypeRepository.findByIsAvailableTrueOrderByPriceAsc().stream()
                .map(GiftTypeResponse::from)
                .toList();
    }

    public List<BillResponse> getBillHistory(String identifier) {
        TableEntity table = resolveTable(identifier);
        return billRepository.findByDeviceIdOrderByCreatedAtDesc(table.getId()).stream()
                .map(this::toBillResponse)
                .toList();
    }

    public List<BillResponse> getAdminBills(BillStatus status) {
        List<Bill> bills = status != null
                ? billRepository.findByStatusOrderByCreatedAtDesc(status)
                : billRepository.findAllByOrderByCreatedAtDesc();
        return bills.stream()
                .map(this::toBillResponse)
                .toList();
    }

    public BillResponse getBillDetail(Long billId) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new BusinessException("bill을 찾을 수 없습니다", HttpStatus.NOT_FOUND, "BILL_003"));
        return toBillResponse(bill);
    }

    public BillResponse getCurrentBill(String identifier) {
        TableEntity table = resolveTable(identifier);
        Bill bill = getOrCreateOpenBill(table);
        return toBillResponse(bill);
    }

    @Transactional
    public BillResponse createOrder(String identifier, CreateOrderRequest request) {
        TableEntity table = resolveTable(identifier);
        Bill bill = getOrCreateOpenBill(table);

        for (OrderItemRequest itemRequest : request.getItems()) {
            MenuItem menuItem = menuItemRepository.findById(itemRequest.getMenuItemId())
                    .filter(MenuItem::getIsAvailable)
                    .orElseThrow(() -> new BusinessException("주문 가능한 메뉴를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "ORDER_001"));

            OrderItem orderItem = OrderItem.builder()
                    .bill(bill)
                    .menuItem(menuItem)
                    .name(menuItem.getName())
                    .price(menuItem.getPrice())
                    .quantity(itemRequest.getQuantity())
                    .category(menuItem.getCategory())
                    .build();
            bill.addOrderItem(orderItem);
        }

        Bill saved = billRepository.save(bill);
        syncTableRevenue(table.getId());
        return toBillResponse(saved);
    }

    @Transactional
    public BillResponse updateOrderItem(String identifier, Long orderItemId, UpdateOrderItemRequest request) {
        TableEntity table = resolveTable(identifier);
        OrderItem orderItem = orderItemRepository.findByIdAndBillDeviceId(orderItemId, table.getId())
                .orElseThrow(() -> new BusinessException("주문 항목을 찾을 수 없습니다", HttpStatus.NOT_FOUND, "ORDER_002"));

        Bill bill = orderItem.getBill();
        validateBillOpen(bill);

        orderItem.setQuantity(request.getQuantity());
        bill.recalculateTotal();

        Bill saved = billRepository.save(bill);
        syncTableRevenue(table.getId());
        return toBillResponse(saved);
    }

    @Transactional
    public BillResponse deleteOrderItem(String identifier, Long orderItemId) {
        TableEntity table = resolveTable(identifier);
        OrderItem orderItem = orderItemRepository.findByIdAndBillDeviceId(orderItemId, table.getId())
                .orElseThrow(() -> new BusinessException("주문 항목을 찾을 수 없습니다", HttpStatus.NOT_FOUND, "ORDER_002"));

        Bill bill = orderItem.getBill();
        validateBillOpen(bill);

        bill.getOrderItems().remove(orderItem);
        bill.recalculateTotal();

        Bill saved = billRepository.save(bill);
        syncTableRevenue(table.getId());
        return toBillResponse(saved);
    }

    @Transactional
    public BillResponse closeBill(String identifier) {
        TableEntity table = resolveTable(identifier);
        Bill bill = billRepository.findByDeviceIdAndStatus(table.getId(), BillStatus.OPEN)
                .orElseThrow(() -> new BusinessException("열려 있는 bill이 없습니다", HttpStatus.NOT_FOUND, "BILL_001"));

        bill.close();
        Bill saved = billRepository.save(bill);
        syncTableRevenue(table.getId());
        return toBillResponse(saved);
    }

    @Transactional
    public void recordGiftOrder(String deviceId, Long chatRoomId, String giftCode) {
        TableEntity table = tableRepository.findById(deviceId)
                .orElseThrow(BusinessException::tableNotFound);
        GiftType giftType = giftTypeRepository.findByCode(giftCode)
                .filter(GiftType::getIsAvailable)
                .orElseThrow(() -> new BusinessException("사용 가능한 선물을 찾을 수 없습니다", HttpStatus.NOT_FOUND, "GIFT_001"));

        Bill bill = getOrCreateOpenBill(table);
        validateBillOpen(bill);

        GiftOrder giftOrder = GiftOrder.builder()
                .bill(bill)
                .giftType(giftType)
                .code(giftType.getCode())
                .displayName(giftType.getDisplayName())
                .price(giftType.getPrice())
                .quantity(1)
                .chatRoomId(chatRoomId)
                .build();
        bill.addGiftOrder(giftOrder);

        billRepository.save(bill);
        syncTableRevenue(table.getId());
    }

    private TableEntity resolveTable(String identifier) {
        return tableRepository.findById(identifier)
                .orElseGet(() -> {
                    List<TableEntity> matched = tableRepository.findByName(identifier).stream()
                            .filter(table -> table.getStatus() != TableStatus.DELETED)
                            .toList();
                    if (matched.isEmpty()) {
                        throw BusinessException.tableNotFound();
                    }
                    if (matched.size() > 1) {
                        throw new BusinessException("동일한 테이블명이 여러 건 존재합니다. deviceId로 요청해주세요", HttpStatus.BAD_REQUEST, "TABLE_006");
                    }
                    return matched.get(0);
                });
    }

    private Bill getOrCreateOpenBill(TableEntity table) {
        return billRepository.findByDeviceIdAndStatus(table.getId(), BillStatus.OPEN)
                .orElseGet(() -> billRepository.save(Bill.builder()
                        .deviceId(table.getId())
                        .tableName(table.getName())
                        .status(BillStatus.OPEN)
                        .build()));
    }

    private void validateBillOpen(Bill bill) {
        if (bill.getStatus() != BillStatus.OPEN) {
            throw new BusinessException("열려 있는 bill에서만 주문을 변경할 수 있습니다", HttpStatus.BAD_REQUEST, "BILL_002");
        }
    }

    private void syncTableRevenue(String deviceId) {
        Long revenue = billRepository.sumTotalAmountByDeviceIdExcludingCancelled(deviceId);
        tableRepository.findById(deviceId).ifPresent(table -> table.setRevenue(revenue != null ? revenue : 0L));
    }

    private BillResponse toBillResponse(Bill bill) {
        Hibernate.initialize(bill.getOrderItems());
        bill.getOrderItems().forEach(item -> Hibernate.initialize(item.getMenuItem()));
        Hibernate.initialize(bill.getGiftOrders());
        bill.getGiftOrders().forEach(giftOrder -> Hibernate.initialize(giftOrder.getGiftType()));
        return BillResponse.from(bill);
    }
}
