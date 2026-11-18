package com.queueless.service;

import com.queueless.entity.*;
import com.queueless.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueServiceTest {

    @Mock
    private QueueRepository queueRepository;
    @Mock
    private QueueTokenRepository queueTokenRepository;
    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CounterRepository counterRepository;
    @Mock
    private QueueEventRepository queueEventRepository;
    @Mock
    private ServiceSessionRepository serviceSessionRepository;
    @Mock
    private QueueEstimationService queueEstimationService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private QueueService queueService;

    private User user;
    private com.queueless.entity.Service service;
    private Queue queue;
    private QueueToken token;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("test@example.com").fullName("Test User").role(Role.USER).build();
        service = com.queueless.entity.Service.builder().id(1L).name("Test Service").prefix("A").defaultEstimatedDurationMinutes(5).build();
        queue = Queue.builder().id(1L).service(service).currentSequence(10).build();
        token = QueueToken.builder()
                .id(100L)
                .tokenNumber("A-010")
                .sequenceNumber(10)
                .queue(queue)
                .user(user)
                .status(TokenStatus.COMPLETED)
                .build();
    }

    @Test
    @DisplayName("Should throw IllegalStateException when starting service on COMPLETED token")
    void testInvalidTransition_CompletedToServing() {
        when(queueTokenRepository.findById(100L)).thenReturn(Optional.of(token));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            queueService.startService(100L, 2L);
        });

        assertTrue(exception.getMessage().contains("Cannot start service for token in status: COMPLETED"));
    }

    @Test
    @DisplayName("Should throw IllegalStateException when calling next on CANCELLED token state transition")
    void testInvalidTransition_CancelledToCalled() {
        token.setStatus(TokenStatus.CANCELLED);
        when(queueTokenRepository.findById(100L)).thenReturn(Optional.of(token));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            queueService.startService(100L, 2L);
        });

        assertTrue(exception.getMessage().contains("Cannot start service for token in status: CANCELLED"));
    }
}
