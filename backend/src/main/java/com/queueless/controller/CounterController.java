package com.queueless.controller;

import com.queueless.dto.ApiResponse;
import com.queueless.dto.CounterDto;
import com.queueless.entity.CounterStatus;
import com.queueless.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.queueless.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/counters")
@RequiredArgsConstructor
@Tag(name = "Counters", description = "Endpoints for counter creation and status updates")
public class CounterController {

    private final OrganizationService organizationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create a counter (Admin)")
    public ResponseEntity<ApiResponse<CounterDto>> createCounter(
            @Valid @RequestBody CounterDto dto,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal != null ? userPrincipal.getId() : null;
        return ResponseEntity.ok(ApiResponse.success(organizationService.createCounter(dto, userId), "Counter created successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete a counter (Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteCounter(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal != null ? userPrincipal.getId() : null;
        organizationService.deleteCounter(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Counter deleted successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update counter status (OPEN, PAUSED, CLOSED)")
    public ResponseEntity<ApiResponse<CounterDto>> updateCounterStatus(
            @PathVariable Long id,
            @RequestParam CounterStatus status,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(ApiResponse.success(organizationService.updateCounterStatus(id, status, userPrincipal.getId()), "Counter status updated"));
    }
}
