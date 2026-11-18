package com.queueless.repository;

import com.queueless.entity.Counter;
import com.queueless.entity.CounterStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CounterRepository extends JpaRepository<Counter, Long> {
    List<Counter> findByDepartmentId(Long departmentId);
    List<Counter> findByDepartmentIdAndStatus(Long departmentId, CounterStatus status);
    List<Counter> findByDepartmentOrganizationId(Long organizationId);
    long countByDepartmentOrganizationIdAndStatus(Long organizationId, CounterStatus status);
}
