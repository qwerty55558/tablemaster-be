package com.mycompany.tablemaster.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "visitor_analytics_logs")
@Getter
@Setter
@NoArgsConstructor
public class VisitorAnalyticsLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private VisitorAnalyticsEventType eventType;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "table_name")
    private String tableName;

    @Column(name = "device_name")
    private String deviceName;

    private String location;

    @Column(name = "guest_count")
    private Integer guestCount;

    @Column(name = "female_count")
    private Integer femaleCount;

    @Column(name = "male_count")
    private Integer maleCount;

    private Long revenue;

    @Enumerated(EnumType.STRING)
    @Column(name = "table_status")
    private TableStatus tableStatus;

    @Column(length = 255)
    private String reason;

    @Column(name = "logged_at", nullable = false)
    private LocalDateTime loggedAt;

    @PrePersist
    protected void onCreate() {
        loggedAt = LocalDateTime.now();
    }

    @Builder
    public VisitorAnalyticsLog(VisitorAnalyticsEventType eventType, String deviceId, String tableName,
                               String deviceName, String location, Integer guestCount, Integer femaleCount,
                               Integer maleCount, Long revenue, TableStatus tableStatus, String reason) {
        this.eventType = eventType;
        this.deviceId = deviceId;
        this.tableName = tableName;
        this.deviceName = deviceName;
        this.location = location;
        this.guestCount = guestCount;
        this.femaleCount = femaleCount;
        this.maleCount = maleCount;
        this.revenue = revenue;
        this.tableStatus = tableStatus;
        this.reason = reason;
    }
}
