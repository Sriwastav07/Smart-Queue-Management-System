package com.queueless.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueueUpdateMessage {
    private String type; // TOKEN_ISSUED, TOKEN_CALLED, SERVING_STARTED, TOKEN_COMPLETED, TOKEN_SKIPPED, TOKEN_CANCELLED, COUNTER_STATUS_CHANGED, LEAVE_RETURN_UPDATED
    private Long queueId;
    private Long serviceId;
    private Long counterId;
    private Long userId;
    private QueueTokenDto token;
    private Integer currentQueueSize;
    private Integer activeCountersCount;
    private String message;
}
