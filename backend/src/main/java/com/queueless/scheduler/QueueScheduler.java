package com.queueless.scheduler;

import com.queueless.entity.QueueToken;
import com.queueless.entity.TokenStatus;
import com.queueless.repository.QueueTokenRepository;
import com.queueless.service.QueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueScheduler {

    private final QueueTokenRepository queueTokenRepository;
    private final QueueService queueService;

    // Run every 1 minute to auto-expire tokens called > 10 minutes ago without response
    @Scheduled(fixedRate = 60000)
    public void cleanupNoShowTokens() {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(10);
        List<QueueToken> timedOutTokens = queueTokenRepository.findAll().stream()
                .filter(t -> t.getStatus() == TokenStatus.CALLED)
                .filter(t -> t.getCalledAt() != null && t.getCalledAt().isBefore(timeoutThreshold))
                .toList();

        for (QueueToken token : timedOutTokens) {
            log.info("Auto marking token {} as NO_SHOW due to 10m call timeout", token.getTokenNumber());
            try {
                queueService.markNoShow(token.getId());
            } catch (Exception e) {
                log.error("Error auto-marking token {} as NO_SHOW", token.getId(), e);
            }
        }
    }
}
