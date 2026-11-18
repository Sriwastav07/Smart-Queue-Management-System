package com.queueless.dto;

import com.queueless.entity.CounterStatus;
import com.queueless.entity.TokenStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueueTokenDto {
    private Long id;
    private String tokenNumber;
    private Integer sequenceNumber;
    private Long queueId;
    private Long serviceId;
    private String serviceName;
    private Long departmentId;
    private String departmentName;
    private Long organizationId;
    private String organizationName;
    private Long userId;
    private String userName;
    private TokenStatus status;
    private Long counterId;
    private String counterName;
    private String counterNumber;
    private Integer peopleAhead;
    private Integer estimatedWaitMinutes;
    private Integer notifyThreshold;
    private Boolean notificationSent;
    private LocalDateTime calledAt;
    private LocalDateTime serviceStartedAt;
    private LocalDateTime completedAt;
    private Integer actualServiceDurationSeconds;
    private LocalDateTime createdAt;
}
