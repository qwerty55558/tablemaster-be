package com.mycompany.tablemaster.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bills")
@Getter
@Setter
@NoArgsConstructor
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "table_name", nullable = false)
    private String tableName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillStatus status = BillStatus.OPEN;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount = 0L;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder
    public Bill(String deviceId, String tableName, BillStatus status) {
        this.deviceId = deviceId;
        this.tableName = tableName;
        this.status = status != null ? status : BillStatus.OPEN;
    }

    public void addOrderItem(OrderItem item) {
        orderItems.add(item);
        item.setBill(this);
        recalculateTotal();
    }

    public void close() {
        this.status = BillStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = BillStatus.CANCELLED;
    }

    public void recalculateTotal() {
        this.totalAmount = orderItems.stream()
                .mapToLong(item -> (long) item.getPrice() * item.getQuantity())
                .sum();
    }
}
