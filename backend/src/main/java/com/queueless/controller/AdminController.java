package com.queueless.controller;

import com.queueless.dto.AdminAnalyticsDto;
import com.queueless.dto.ApiResponse;
import com.queueless.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.queueless.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Admin Analytics", description = "Endpoints for admin organizational metrics and analytics")
public class AdminController {

    private final OrganizationService organizationService;

    @GetMapping("/analytics/organizations/{orgId}")
    @Operation(summary = "Get admin analytics dashboard data for organization")
    public ResponseEntity<ApiResponse<AdminAnalyticsDto>> getAdminAnalytics(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(ApiResponse.success(organizationService.getAdminAnalytics(orgId, userPrincipal.getId())));
    }
}
