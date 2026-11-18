package com.queueless.service;

import com.queueless.dto.QueuePredictionDto;
import com.queueless.dto.QueuePredictionDto.HourlyCrowdForecast;
import com.queueless.entity.Service;
import com.queueless.repository.QueueTokenRepository;
import com.queueless.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class QueuePredictionService {

    private final ServiceRepository serviceRepository;
    private final QueueTokenRepository queueTokenRepository;

    public QueuePredictionDto getPredictionForService(Long serviceId) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        LocalDateTime now = LocalDateTime.now();
        String dayOfWeek = now.getDayOfWeek().name();
        int currentHour = now.getHour();

        List<HourlyCrowdForecast> forecasts = new ArrayList<>();
        int peakHour = 10;
        int maxCrowd = 0;

        for (int hour = 8; hour <= 17; hour++) {
            String timeSlot = String.format("%02d:00 - %02d:00", hour, hour + 1);
            int crowd = calculateHistoricalMultiplier(service, hour);
            int waitTime = (int) Math.ceil((crowd * service.getDefaultEstimatedDurationMinutes()) / 2.0);

            String crowdLevel = "LOW";
            if (crowd > 60) crowdLevel = "PEAK";
            else if (crowd > 35) crowdLevel = "HIGH";
            else if (crowd > 15) crowdLevel = "MODERATE";

            if (crowd > maxCrowd) {
                maxCrowd = crowd;
                peakHour = hour;
            }

            forecasts.add(new HourlyCrowdForecast(timeSlot, crowd, waitTime, crowdLevel));
        }

        String recommendation;
        if (maxCrowd > 50) {
            recommendation = String.format("High peak expected around %02d:00. Recommendation: Open at least 2 additional counters.", peakHour);
        } else {
            recommendation = "Normal crowd flow expected. Current counter allocation is sufficient.";
        }

        return QueuePredictionDto.builder()
                .serviceId(service.getId())
                .serviceName(service.getName())
                .dayOfWeek(dayOfWeek)
                .hourOfDay(currentHour)
                .expectedQueueSize(calculateHistoricalMultiplier(service, currentHour))
                .expectedWaitTimeMinutes(calculateHistoricalMultiplier(service, currentHour) * service.getDefaultEstimatedDurationMinutes() / 2)
                .recommendation(recommendation)
                .hourlyForecasts(forecasts)
                .build();
    }

    private int calculateHistoricalMultiplier(Service service, int hour) {
        if (hour == 10 || hour == 11) return 75 + (int)(service.getId() % 10);
        if (hour == 14 || hour == 15) return 60 + (int)(service.getId() % 10);
        if (hour == 9 || hour == 12) return 40 + (int)(service.getId() % 5);
        if (hour == 13 || hour == 16) return 25 + (int)(service.getId() % 5);
        return 10;
    }
}
