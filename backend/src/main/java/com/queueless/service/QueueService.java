package com.queueless.service;

import com.queueless.dto.QueueTokenDto;
import com.queueless.dto.QueueUpdateMessage;
import com.queueless.entity.*;
import com.queueless.entity.Queue;
import com.queueless.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueService {

    private final QueueRepository queueRepository;
    private final QueueTokenRepository queueTokenRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final CounterRepository counterRepository;
    private final QueueEventRepository queueEventRepository;
    private final ServiceSessionRepository serviceSessionRepository;
    private final QueueEstimationService queueEstimationService;
    private final NotificationService notificationService;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public synchronized QueueTokenDto issueToken(Long userId, Long serviceId, Integer notifyThreshold) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        com.queueless.entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        Optional<QueueToken> existingActive = queueTokenRepository.findFirstByUserIdAndStatusInOrderByCreatedAtDesc(
                userId, List.of(TokenStatus.WAITING, TokenStatus.CALLED, TokenStatus.SERVING)
        );

        if (existingActive.isPresent() && existingActive.get().getQueue().getService().getId().equals(serviceId)) {
            throw new IllegalStateException("You already have an active token for this service (" + existingActive.get().getTokenNumber() + ")");
        }

        Queue queue = queueRepository.findByServiceId(serviceId)
                .orElseGet(() -> queueRepository.save(Queue.builder().service(service).currentSequence(0).build()));

        Queue lockedQueue = queueRepository.findByIdForUpdate(queue.getId())
                .orElse(queue);

        int nextSeq = lockedQueue.getCurrentSequence() + 1;
        lockedQueue.setCurrentSequence(nextSeq);
        queueRepository.saveAndFlush(lockedQueue);

        String tokenNumber = service.getPrefix() + "-" + String.format("%03d", nextSeq);

        QueueToken token = QueueToken.builder()
                .tokenNumber(tokenNumber)
                .sequenceNumber(nextSeq)
                .queue(lockedQueue)
                .user(user)
                .status(TokenStatus.WAITING)
                .notifyThreshold(notifyThreshold != null ? notifyThreshold : 10)
                .notificationSent(false)
                .build();

        QueueToken savedToken = queueTokenRepository.saveAndFlush(token);

        queueEventRepository.save(QueueEvent.builder()
                .queue(lockedQueue)
                .token(savedToken)
                .eventType(TokenStatus.WAITING)
                .details("Token " + tokenNumber + " issued to user " + user.getFullName())
                .build());

        QueueTokenDto dto = mapToDto(savedToken);

        notificationService.createAndSendNotification(
                user,
                "Token Issued: " + tokenNumber,
                "You joined the queue for " + service.getName() + ". Your token is " + tokenNumber + ".",
                NotificationType.QUEUE_JOINED,
                savedToken.getId()
        );

        QueueUpdateMessage msg = QueueUpdateMessage.builder()
                .type("TOKEN_ISSUED")
                .queueId(lockedQueue.getId())
                .serviceId(service.getId())
                .userId(userId)
                .token(dto)
                .message("New token issued: " + tokenNumber)
                .build();

        notificationService.broadcastQueueUpdate(lockedQueue.getId(), msg);

        return dto;
    }

    @Transactional
    public QueueTokenDto callNextToken(Long counterId, Long staffId) {
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new IllegalArgumentException("Counter not found"));

        if (staffId != null) {
            User staff = userRepository.findById(staffId).orElse(null);
            if (staff != null && staff.getRole() == Role.STAFF && staff.getOrganization() != null) {
                if (!counter.getDepartment().getOrganization().getId().equals(staff.getOrganization().getId())) {
                    throw new IllegalArgumentException("Unauthorized: You can only call tokens for counters in your assigned organization");
                }
            }
        }

        if (counter.getStatus() != CounterStatus.OPEN) {
            throw new IllegalStateException("Counter is not open");
        }

        List<com.queueless.entity.Service> deptServices = serviceRepository.findByDepartmentIdAndActiveTrue(counter.getDepartment().getId());
        List<com.queueless.entity.Service> orgServices = serviceRepository.findByDepartmentOrganizationIdAndActiveTrue(counter.getDepartment().getOrganization().getId());

        Set<Long> addedIds = new HashSet<>();
        List<com.queueless.entity.Service> services = new ArrayList<>();
        for (com.queueless.entity.Service s : deptServices) {
            services.add(s);
            addedIds.add(s.getId());
        }
        for (com.queueless.entity.Service s : orgServices) {
            if (!addedIds.contains(s.getId())) {
                services.add(s);
                addedIds.add(s.getId());
            }
        }

        if (services.isEmpty()) {
            throw new IllegalStateException("No active services for counter's organization");
        }

        QueueToken nextToken = null;
        for (com.queueless.entity.Service service : services) {
            Optional<Queue> queueOpt = queueRepository.findByServiceId(service.getId());
            if (queueOpt.isPresent()) {
                List<QueueToken> waiting = queueTokenRepository.findByQueueIdAndStatusOrderBySequenceNumberAsc(queueOpt.get().getId(), TokenStatus.WAITING);
                if (!waiting.isEmpty()) {
                    nextToken = waiting.get(0);
                    break;
                }
            }
        }

        if (nextToken == null) {
            throw new IllegalStateException("No waiting tokens in the queue");
        }

        nextToken.setStatus(TokenStatus.CALLED);
        nextToken.setCounter(counter);
        nextToken.setCalledAt(LocalDateTime.now());
        QueueToken saved = queueTokenRepository.save(nextToken);

        queueEventRepository.save(QueueEvent.builder()
                .queue(saved.getQueue())
                .token(saved)
                .eventType(TokenStatus.CALLED)
                .details("Token " + saved.getTokenNumber() + " called at counter " + counter.getName())
                .build());

        QueueTokenDto dto = mapToDto(saved);

        notificationService.createAndSendNotification(
                saved.getUser(),
                "Your Turn! Token " + saved.getTokenNumber(),
                "Please proceed to " + counter.getName() + " immediately.",
                NotificationType.TOKEN_CALLED,
                saved.getId()
        );

        updateAndNotifyQueueBehind(saved.getQueue());

        QueueUpdateMessage msg = QueueUpdateMessage.builder()
                .type("TOKEN_CALLED")
                .queueId(saved.getQueue().getId())
                .serviceId(saved.getQueue().getService().getId())
                .counterId(counterId)
                .userId(saved.getUser().getId())
                .token(dto)
                .message("Token " + saved.getTokenNumber() + " called to " + counter.getName())
                .build();

        notificationService.broadcastQueueUpdate(saved.getQueue().getId(), msg);
        notificationService.broadcastUserUpdate(saved.getUser().getId(), msg);
        notificationService.broadcastCounterUpdate(counterId, msg);

        return dto;
    }

    @Transactional(readOnly = true)
    public List<QueueTokenDto> getWaitingTokensForCounter(Long counterId) {
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new IllegalArgumentException("Counter not found"));

        List<com.queueless.entity.Service> deptServices = serviceRepository.findByDepartmentIdAndActiveTrue(counter.getDepartment().getId());
        List<com.queueless.entity.Service> orgServices = serviceRepository.findByDepartmentOrganizationIdAndActiveTrue(counter.getDepartment().getOrganization().getId());

        Set<Long> addedIds = new HashSet<>();
        List<com.queueless.entity.Service> services = new ArrayList<>();
        for (com.queueless.entity.Service s : deptServices) {
            services.add(s);
            addedIds.add(s.getId());
        }
        for (com.queueless.entity.Service s : orgServices) {
            if (!addedIds.contains(s.getId())) {
                services.add(s);
                addedIds.add(s.getId());
            }
        }

        List<QueueTokenDto> result = new ArrayList<>();
        for (com.queueless.entity.Service service : services) {
            Optional<Queue> queueOpt = queueRepository.findByServiceId(service.getId());
            if (queueOpt.isPresent()) {
                List<QueueToken> waiting = queueTokenRepository.findByQueueIdAndStatusOrderBySequenceNumberAsc(queueOpt.get().getId(), TokenStatus.WAITING);
                for (QueueToken t : waiting) {
                    result.add(mapToDto(t));
                }
            }
        }
        return result;
    }

    @Transactional
    public QueueTokenDto startService(Long tokenId, Long staffId) {
        QueueToken token = queueTokenRepository.findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Token not found"));

        if (token.getStatus() != TokenStatus.CALLED && token.getStatus() != TokenStatus.WAITING) {
            throw new IllegalStateException("Cannot start service for token in status: " + token.getStatus());
        }

        token.setStatus(TokenStatus.SERVING);
        token.setServiceStartedAt(LocalDateTime.now());
        QueueToken saved = queueTokenRepository.save(token);

        queueEventRepository.save(QueueEvent.builder()
                .queue(saved.getQueue())
                .token(saved)
                .eventType(TokenStatus.SERVING)
                .details("Serving started for token " + saved.getTokenNumber())
                .build());

        QueueTokenDto dto = mapToDto(saved);

        QueueUpdateMessage msg = QueueUpdateMessage.builder()
                .type("SERVING_STARTED")
                .queueId(saved.getQueue().getId())
                .serviceId(saved.getQueue().getService().getId())
                .counterId(saved.getCounter() != null ? saved.getCounter().getId() : null)
                .userId(saved.getUser().getId())
                .token(dto)
                .message("Service started for " + saved.getTokenNumber())
                .build();

        notificationService.broadcastQueueUpdate(saved.getQueue().getId(), msg);
        notificationService.broadcastUserUpdate(saved.getUser().getId(), msg);

        return dto;
    }

    @Transactional
    public QueueTokenDto completeToken(Long tokenId, Long staffId) {
        QueueToken token = queueTokenRepository.findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Token not found"));

        if (token.getStatus() != TokenStatus.SERVING && token.getStatus() != TokenStatus.CALLED) {
            throw new IllegalStateException("Cannot complete token in status: " + token.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        token.setStatus(TokenStatus.COMPLETED);
        token.setCompletedAt(now);

        LocalDateTime startTime = token.getServiceStartedAt() != null ? token.getServiceStartedAt() : token.getCalledAt();
        if (startTime != null) {
            long duration = Duration.between(startTime, now).getSeconds();
            token.setActualServiceDurationSeconds((int) duration);
        } else {
            token.setActualServiceDurationSeconds(300);
        }

        QueueToken saved = queueTokenRepository.save(token);

        if (saved.getCounter() != null && staffId != null) {
            User staff = userRepository.findById(staffId).orElse(null);
            if (staff != null) {
                serviceSessionRepository.save(ServiceSession.builder()
                        .counter(saved.getCounter())
                        .staff(staff)
                        .token(saved)
                        .startTime(startTime)
                        .endTime(now)
                        .durationSeconds(saved.getActualServiceDurationSeconds())
                        .build());
            }
        }

        queueEventRepository.save(QueueEvent.builder()
                .queue(saved.getQueue())
                .token(saved)
                .eventType(TokenStatus.COMPLETED)
                .details("Token " + saved.getTokenNumber() + " completed service.")
                .build());

        QueueTokenDto dto = mapToDto(saved);

        notificationService.createAndSendNotification(
                saved.getUser(),
                "Service Completed",
                "Thank you! Your service for " + saved.getQueue().getService().getName() + " is complete.",
                NotificationType.TOKEN_COMPLETED,
                saved.getId()
        );

        updateAndNotifyQueueBehind(saved.getQueue());

        QueueUpdateMessage msg = QueueUpdateMessage.builder()
                .type("TOKEN_COMPLETED")
                .queueId(saved.getQueue().getId())
                .serviceId(saved.getQueue().getService().getId())
                .counterId(saved.getCounter() != null ? saved.getCounter().getId() : null)
                .userId(saved.getUser().getId())
                .token(dto)
                .message("Token " + saved.getTokenNumber() + " completed")
                .build();

        notificationService.broadcastQueueUpdate(saved.getQueue().getId(), msg);
        notificationService.broadcastUserUpdate(saved.getUser().getId(), msg);

        return dto;
    }

    @Transactional
    public QueueTokenDto skipToken(Long tokenId) {
        QueueToken token = queueTokenRepository.findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Token not found"));

        if (token.getStatus() != TokenStatus.CALLED && token.getStatus() != TokenStatus.WAITING) {
            throw new IllegalStateException("Cannot skip token in status: " + token.getStatus());
        }

        token.setStatus(TokenStatus.SKIPPED);
        QueueToken saved = queueTokenRepository.save(token);

        queueEventRepository.save(QueueEvent.builder()
                .queue(saved.getQueue())
                .token(saved)
                .eventType(TokenStatus.SKIPPED)
                .details("Token " + saved.getTokenNumber() + " was skipped.")
                .build());

        QueueTokenDto dto = mapToDto(saved);

        notificationService.createAndSendNotification(
                saved.getUser(),
                "Token Skipped",
                "Your token " + saved.getTokenNumber() + " was skipped because you were not present.",
                NotificationType.TOKEN_SKIPPED,
                saved.getId()
        );

        updateAndNotifyQueueBehind(saved.getQueue());

        QueueUpdateMessage msg = QueueUpdateMessage.builder()
                .type("TOKEN_SKIPPED")
                .queueId(saved.getQueue().getId())
                .serviceId(saved.getQueue().getService().getId())
                .userId(saved.getUser().getId())
                .token(dto)
                .message("Token " + saved.getTokenNumber() + " skipped")
                .build();

        notificationService.broadcastQueueUpdate(saved.getQueue().getId(), msg);
        notificationService.broadcastUserUpdate(saved.getUser().getId(), msg);

        return dto;
    }

    @Transactional
    public QueueTokenDto recallSkippedToken(Long tokenId) {
        QueueToken token = queueTokenRepository.findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Token not found"));

        if (token.getStatus() != TokenStatus.SKIPPED) {
            throw new IllegalStateException("Only SKIPPED tokens can be recalled");
        }

        token.setStatus(TokenStatus.WAITING);
        QueueToken saved = queueTokenRepository.save(token);

        QueueTokenDto dto = mapToDto(saved);

        QueueUpdateMessage msg = QueueUpdateMessage.builder()
                .type("TOKEN_RECALLED")
                .queueId(saved.getQueue().getId())
                .serviceId(saved.getQueue().getService().getId())
                .userId(saved.getUser().getId())
                .token(dto)
                .message("Token " + saved.getTokenNumber() + " added back to waiting queue")
                .build();

        notificationService.broadcastQueueUpdate(saved.getQueue().getId(), msg);
        notificationService.broadcastUserUpdate(saved.getUser().getId(), msg);

        return dto;
    }

    @Transactional
    public QueueTokenDto markNoShow(Long tokenId) {
        QueueToken token = queueTokenRepository.findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Token not found"));

        token.setStatus(TokenStatus.NO_SHOW);
        QueueToken saved = queueTokenRepository.save(token);

        QueueTokenDto dto = mapToDto(saved);

        QueueUpdateMessage msg = QueueUpdateMessage.builder()
                .type("TOKEN_NO_SHOW")
                .queueId(saved.getQueue().getId())
                .serviceId(saved.getQueue().getService().getId())
                .userId(saved.getUser().getId())
                .token(dto)
                .message("Token " + saved.getTokenNumber() + " marked as NO_SHOW")
                .build();

        notificationService.broadcastQueueUpdate(saved.getQueue().getId(), msg);
        notificationService.broadcastUserUpdate(saved.getUser().getId(), msg);

        return dto;
    }

    @Transactional
    public QueueTokenDto cancelToken(Long tokenId, Long userId) {
        QueueToken token = queueTokenRepository.findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Token not found"));

        if (!token.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized to cancel this token");
        }

        if (token.getStatus() == TokenStatus.COMPLETED || token.getStatus() == TokenStatus.CANCELLED) {
            throw new IllegalStateException("Token is already " + token.getStatus());
        }

        token.setStatus(TokenStatus.CANCELLED);
        QueueToken saved = queueTokenRepository.save(token);

        queueEventRepository.save(QueueEvent.builder()
                .queue(saved.getQueue())
                .token(saved)
                .eventType(TokenStatus.CANCELLED)
                .details("Token " + saved.getTokenNumber() + " was cancelled by user.")
                .build());

        QueueTokenDto dto = mapToDto(saved);

        updateAndNotifyQueueBehind(saved.getQueue());

        QueueUpdateMessage msg = QueueUpdateMessage.builder()
                .type("TOKEN_CANCELLED")
                .queueId(saved.getQueue().getId())
                .serviceId(saved.getQueue().getService().getId())
                .userId(userId)
                .token(dto)
                .message("Token " + saved.getTokenNumber() + " cancelled")
                .build();

        notificationService.broadcastQueueUpdate(saved.getQueue().getId(), msg);

        return dto;
    }

    @Transactional
    public QueueTokenDto setLeaveAndReturnThreshold(Long tokenId, Long userId, Integer threshold) {
        QueueToken token = queueTokenRepository.findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Token not found"));

        if (!token.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized modification");
        }

        token.setNotifyThreshold(threshold);
        token.setNotificationSent(false);
        QueueToken saved = queueTokenRepository.save(token);

        QueueTokenDto dto = mapToDto(saved);

        QueueUpdateMessage msg = QueueUpdateMessage.builder()
                .type("LEAVE_RETURN_UPDATED")
                .queueId(saved.getQueue().getId())
                .serviceId(saved.getQueue().getService().getId())
                .userId(userId)
                .token(dto)
                .message("Notification threshold set to " + threshold + " people ahead")
                .build();

        notificationService.broadcastUserUpdate(userId, msg);

        return dto;
    }

    @Transactional(readOnly = true)
    public QueueTokenDto getActiveTokenForUser(Long userId) {
        Optional<QueueToken> activeOpt = queueTokenRepository.findFirstByUserIdAndStatusInOrderByCreatedAtDesc(
                userId, List.of(TokenStatus.WAITING, TokenStatus.CALLED, TokenStatus.SERVING)
        );
        return activeOpt.map(this::mapToDto).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<QueueTokenDto> getUserTokenHistory(Long userId) {
        return queueTokenRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<QueueTokenDto> getQueueTokens(Long queueId) {
        return queueTokenRepository.findByQueueIdAndStatusInOrderBySequenceNumberAsc(
                        queueId, List.of(TokenStatus.WAITING, TokenStatus.CALLED, TokenStatus.SERVING)
                )
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private void updateAndNotifyQueueBehind(Queue queue) {
        List<QueueToken> waitingTokens = queueTokenRepository.findByQueueIdAndStatusOrderBySequenceNumberAsc(queue.getId(), TokenStatus.WAITING);
        for (QueueToken waitingToken : waitingTokens) {
            long peopleAhead = queueTokenRepository.countPeopleAhead(queue.getId(), waitingToken.getSequenceNumber());
            if (peopleAhead <= waitingToken.getNotifyThreshold() && !Boolean.TRUE.equals(waitingToken.getNotificationSent())) {
                waitingToken.setNotificationSent(true);
                queueTokenRepository.save(waitingToken);

                notificationService.createAndSendNotification(
                        waitingToken.getUser(),
                        "Your turn is approaching!",
                        "Only " + peopleAhead + " people ahead of your token " + waitingToken.getTokenNumber() + ". Please start returning to the venue.",
                        NotificationType.APPROACHING_TURN,
                        waitingToken.getId()
                );
            }
        }
    }

    public QueueTokenDto mapToDto(QueueToken token) {
        long peopleAhead = 0;
        int estWait = 0;
        if (token.getStatus() == TokenStatus.WAITING) {
            peopleAhead = queueTokenRepository.countPeopleAhead(token.getQueue().getId(), token.getSequenceNumber());
            estWait = queueEstimationService.calculateEstimatedWaitTimeMinutes(token.getQueue(), token);
        }

        return QueueTokenDto.builder()
                .id(token.getId())
                .tokenNumber(token.getTokenNumber())
                .sequenceNumber(token.getSequenceNumber())
                .queueId(token.getQueue().getId())
                .serviceId(token.getQueue().getService().getId())
                .serviceName(token.getQueue().getService().getName())
                .departmentId(token.getQueue().getService().getDepartment().getId())
                .departmentName(token.getQueue().getService().getDepartment().getName())
                .organizationId(token.getQueue().getService().getDepartment().getOrganization().getId())
                .organizationName(token.getQueue().getService().getDepartment().getOrganization().getName())
                .userId(token.getUser().getId())
                .userName(token.getUser().getFullName())
                .status(token.getStatus())
                .counterId(token.getCounter() != null ? token.getCounter().getId() : null)
                .counterName(token.getCounter() != null ? token.getCounter().getName() : null)
                .counterNumber(token.getCounter() != null ? token.getCounter().getCounterNumber() : null)
                .peopleAhead((int) peopleAhead)
                .estimatedWaitMinutes(estWait)
                .notifyThreshold(token.getNotifyThreshold())
                .notificationSent(token.getNotificationSent())
                .calledAt(token.getCalledAt())
                .serviceStartedAt(token.getServiceStartedAt())
                .completedAt(token.getCompletedAt())
                .actualServiceDurationSeconds(token.getActualServiceDurationSeconds())
                .createdAt(token.getCreatedAt())
                .build();
    }
}
