package com.queueless.service;

import com.queueless.dto.NotificationDto;
import com.queueless.dto.QueueUpdateMessage;
import com.queueless.entity.Notification;
import com.queueless.entity.NotificationType;
import com.queueless.entity.User;
import com.queueless.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public NotificationDto createAndSendNotification(User user, String title, String message, NotificationType type, Long tokenTokenId) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .queueTokenId(tokenTokenId)
                .read(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        NotificationDto dto = mapToDto(saved);

        // Broadcast to user-specific WS endpoint
        try {
            messagingTemplate.convertAndSend("/topic/user/" + user.getId() + "/notifications", dto);
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification to user {}", user.getId(), e);
        }

        return dto;
    }

    public void broadcastQueueUpdate(Long queueId, QueueUpdateMessage updateMessage) {
        try {
            messagingTemplate.convertAndSend("/topic/queue/" + queueId, updateMessage);
        } catch (Exception e) {
            log.error("Failed to broadcast queue update to /topic/queue/{}", queueId, e);
        }
    }

    public void broadcastUserUpdate(Long userId, QueueUpdateMessage updateMessage) {
        try {
            messagingTemplate.convertAndSend("/topic/user/" + userId, updateMessage);
        } catch (Exception e) {
            log.error("Failed to broadcast user update to /topic/user/{}", userId, e);
        }
    }

    public void broadcastCounterUpdate(Long counterId, QueueUpdateMessage updateMessage) {
        try {
            messagingTemplate.convertAndSend("/topic/counter/" + counterId, updateMessage);
        } catch (Exception e) {
            log.error("Failed to broadcast counter update to /topic/counter/{}", counterId, e);
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access to notification");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        notifications.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    public NotificationDto mapToDto(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .userId(notification.getUser().getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .read(notification.getRead())
                .queueTokenId(notification.getQueueTokenId())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
