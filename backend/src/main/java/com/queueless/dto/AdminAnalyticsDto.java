package com.queueless.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAnalyticsDto {
    private Long organizationId;
    private String organizationName;
    private Long totalCustomersToday;
    private Long currentlyWaiting;
    private Integer averageWaitMinutes;
    private Long completedToday;
    private Long noShowsToday;
    private Long cancelledToday;
    private Long activeCountersCount;
    private Long totalCountersCount;

    private List<HourlyQueueStat> hourlyQueueStats;
    private List<ServiceDemandStat> serviceWiseDemand;
    private List<CounterPerformanceStat> counterPerformance;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HourlyQueueStat {
        private String hourLabel;
        private Long joinedCount;
        private Long servedCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServiceDemandStat {
        private String serviceName;
        private Long count;
        private Integer averageDurationMinutes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CounterPerformanceStat {
        private String counterName;
        private String staffName;
        private Long tokensServed;
        private Integer avgServiceDurationSeconds;
    }
}
