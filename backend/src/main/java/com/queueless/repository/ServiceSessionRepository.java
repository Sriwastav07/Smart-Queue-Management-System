package com.queueless.repository;

import com.queueless.entity.ServiceSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceSessionRepository extends JpaRepository<ServiceSession, Long> {
    List<ServiceSession> findByStaffIdOrderByStartTimeDesc(Long staffId);
    List<ServiceSession> findByCounterIdOrderByStartTimeDesc(Long counterId);
}
