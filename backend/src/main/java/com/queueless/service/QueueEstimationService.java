package com.queueless.service;

import com.queueless.entity.*;
import com.queueless.repository.CounterRepository;
import com.queueless.repository.QueueTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueEstimationService {

    private final QueueTokenRepository queueTokenRepository;
    private final CounterRepository counterRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional(readOnly = true)
    public int calculateEstimatedWaitTimeMinutes(Queue queue, QueueToken targetToken) {
        if (targetToken != null && targetToken.getStatus() != TokenStatus.WAITING) {
            return 0;
        }

        long peopleAhead = 0;
        if (targetToken != null) {
            peopleAhead = queueTokenRepository.countPeopleAhead(queue.getId(), targetToken.getSequenceNumber());
        } else {
            peopleAhead = queueTokenRepository.countByQueueIdAndStatus(queue.getId(), TokenStatus.WAITING);
        }

        if (peopleAhead <= 0) {
            return 0;
        }

        long activeCounters = counterRepository.countByDepartmentOrganizationIdAndStatus(
                queue.getService().getDepartment().getOrganization().getId(), CounterStatus.OPEN
        );
        if (activeCounters <= 0) {
            activeCounters = 1; // Default to 1 to avoid division by zero
        }

        double avgServiceSeconds = getAverageServiceDurationSeconds(queue.getService());
        double totalWaitSeconds = (peopleAhead * avgServiceSeconds) / (double) activeCounters;

        int waitMinutes = (int) Math.ceil(totalWaitSeconds / 60.0);
        return Math.max(1, waitMinutes);
    }

    public double getAverageServiceDurationSeconds(com.queueless.entity.Service service) {
        String cacheKey = "queue:avg_duration:" + service.getId();
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return Double.parseDouble(cached.toString());
            }
        } catch (Exception e) {
            log.warn("Redis unavailable for avg service duration cache: {}", e.getMessage());
        }

        Double dbAvg = queueTokenRepository.findAverageServiceDurationSecondsByServiceId(service.getId());
        double finalAvg;
        if (dbAvg != null && dbAvg > 0) {
            finalAvg = dbAvg;
        } else {
            finalAvg = service.getDefaultEstimatedDurationMinutes() * 60.0;
        }

        try {
            redisTemplate.opsForValue().set(cacheKey, String.valueOf(finalAvg), Duration.ofMinutes(15));
        } catch (Exception e) {
            log.warn("Failed to set Redis cache for avg service duration: {}", e.getMessage());
        }

        return finalAvg;
    }
}
