package com.mycompany.tablemaster.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "table_history")
@Getter
@NoArgsConstructor
public class TableHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String deviceId;

    @Column(nullable = false)
    private String name;

    @Column(name = "device_name")
    private String deviceName;

    private String location;

    private Integer guestCount;

    private Integer femaleCount;

    private Integer maleCount;

    private Long revenue;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public TableHistory(String deviceId, String name, String deviceName, String location,
                        Integer guestCount, Integer femaleCount, Integer maleCount,
                        Long revenue, LocalDateTime createdAt) {
        this.deviceId = deviceId;
        this.name = name;
        this.deviceName = deviceName;
        this.location = location;
        this.guestCount = guestCount;
        this.femaleCount = femaleCount;
        this.maleCount = maleCount;
        this.revenue = revenue;
        this.createdAt = createdAt;
        this.deletedAt = LocalDateTime.now();
    }

    public static TableHistory from(TableEntity table) {
        return TableHistory.builder()
                .deviceId(table.getId())
                .name(table.getName())
                .deviceName(table.getDeviceName())
                .location(table.getLocation())
                .guestCount(table.getGuestCount())
                .femaleCount(table.getFemaleCount())
                .maleCount(table.getMaleCount())
                .revenue(table.getRevenue())
                .createdAt(table.getCreatedAt())
                .build();
    }
}
