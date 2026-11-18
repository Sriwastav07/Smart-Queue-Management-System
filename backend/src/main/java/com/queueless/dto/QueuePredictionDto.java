package com.queueless.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueuePredictionDto {
    private Long serviceId;
    private String serviceName;
    private String dayOfWeek;
    private Integer hourOfDay;
    private Integer expectedQueueSize;
    private Integer expectedWaitTimeMinutes;
    private String recommendation;
    private List<HourlyCrowdForecast> hourlyForecasts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HourlyCrowdForecast {
        private String timeSlot; // e.g., "10:00 AM - 11:00 AM"
        private Integer expectedCrowd;
        private Integer expectedWaitMinutes;
        private String crowdLevel; // LOW, MODERATE, HIGH, PEAK
    }
}
