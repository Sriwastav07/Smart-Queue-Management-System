package com.queueless.repository;

import com.queueless.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    List<Organization> findByActiveTrue();
    
    @Query("SELECT DISTINCT o.category FROM Organization o WHERE o.active = true")
    List<String> findDistinctCategories();
}
