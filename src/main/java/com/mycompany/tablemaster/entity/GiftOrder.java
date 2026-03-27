package com.mycompany.tablemaster.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "gift_orders")
@Getter
@Setter
@NoArgsConstructor
public class GiftOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gift_type_id", nullable = false)
    private GiftType giftType;

    @Column(nullable = false)
    private String code;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "chat_room_id")
    private Long chatRoomId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Builder
    public GiftOrder(Bill bill, GiftType giftType, String code, String displayName,
                     Integer price, Integer quantity, Long chatRoomId) {
        this.bill = bill;
        this.giftType = giftType;
        this.code = code;
        this.displayName = displayName;
        this.price = price;
        this.quantity = quantity;
        this.chatRoomId = chatRoomId;
    }
}
