package com.queueless.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "daily_queue_statistics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyQueueStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(nullable = false)
    private LocalDate date;

    @Builder.Default
    private Integer totalTokensIssued = 0;

    @Builder.Default
    private Integer totalServed = 0;

    @Builder.Default
    private Integer totalCancelled = 0;

    @Builder.Default
    private Integer totalNoShow = 0;

    @Builder.Default
    private Integer averageWaitTimeMinutes = 0;

    @Builder.Default
    private Integer averageServiceDurationSeconds = 0;

    private Integer peakHour; // e.g., 10 for 10:00 AM
}
