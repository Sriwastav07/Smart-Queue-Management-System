package com.queueless.repository;

import com.queueless.entity.StaffAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffAssignmentRepository extends JpaRepository<StaffAssignment, Long> {
    List<StaffAssignment> findByStaffIdAndActiveTrue(Long staffId);
    Optional<StaffAssignment> findByCounterIdAndActiveTrue(Long counterId);
    List<StaffAssignment> findByCounterId(Long counterId);
    List<StaffAssignment> findByCounterDepartmentOrganizationIdAndActiveTrue(Long organizationId);
}
