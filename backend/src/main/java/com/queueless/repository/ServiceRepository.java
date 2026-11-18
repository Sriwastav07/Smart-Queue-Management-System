package com.queueless.repository;

import com.queueless.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRepository extends JpaRepository<Service, Long> {
    List<Service> findByDepartmentIdAndActiveTrue(Long departmentId);
    List<Service> findByDepartmentOrganizationIdAndActiveTrue(Long organizationId);
}
