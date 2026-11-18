package com.queueless.repository;

import com.queueless.entity.QueueEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QueueEventRepository extends JpaRepository<QueueEvent, Long> {
    List<QueueEvent> findByQueueIdOrderByTimestampDesc(Long queueId);
}
