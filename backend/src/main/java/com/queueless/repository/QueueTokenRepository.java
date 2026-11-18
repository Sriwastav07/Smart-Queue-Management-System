package com.queueless.repository;

import com.queueless.entity.QueueToken;
import com.queueless.entity.TokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QueueTokenRepository extends JpaRepository<QueueToken, Long> {

    List<QueueToken> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<QueueToken> findFirstByUserIdAndStatusInOrderByCreatedAtDesc(Long userId, List<TokenStatus> statuses);

    List<QueueToken> findByQueueIdAndStatusOrderBySequenceNumberAsc(Long queueId, TokenStatus status);

    List<QueueToken> findByQueueIdAndStatusInOrderBySequenceNumberAsc(Long queueId, List<TokenStatus> statuses);

    long countByQueueIdAndStatus(Long queueId, TokenStatus status);

    long countByQueueIdAndStatusIn(Long queueId, List<TokenStatus> statuses);

    @Query("SELECT COUNT(t) FROM QueueToken t WHERE t.queue.id = :queueId AND t.status = 'WAITING' AND t.sequenceNumber < :sequenceNumber")
    long countPeopleAhead(@Param("queueId") Long queueId, @Param("sequenceNumber") Integer sequenceNumber);

    List<QueueToken> findByCounterIdAndStatus(Long counterId, TokenStatus status);

    List<QueueToken> findByCounterId(Long counterId);

    Optional<QueueToken> findFirstByCounterIdAndStatusOrderByCalledAtDesc(Long counterId, TokenStatus status);

    @Query("SELECT AVG(t.actualServiceDurationSeconds) FROM QueueToken t WHERE t.queue.service.id = :serviceId AND t.status = 'COMPLETED' AND t.actualServiceDurationSeconds IS NOT NULL")
    Double findAverageServiceDurationSecondsByServiceId(@Param("serviceId") Long serviceId);

    @Query("SELECT COUNT(t) FROM QueueToken t WHERE t.queue.service.department.organization.id = :orgId AND t.status = :status AND t.createdAt >= :startDate")
    long countByOrganizationAndStatusAndCreatedAtAfter(@Param("orgId") Long orgId, @Param("status") TokenStatus status, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(t) FROM QueueToken t WHERE t.queue.service.department.organization.id = :orgId AND t.createdAt >= :startDate")
    long countByOrganizationAndCreatedAtAfter(@Param("orgId") Long orgId, @Param("startDate") LocalDateTime startDate);
}
