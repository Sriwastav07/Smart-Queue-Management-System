package com.queueless.controller;

import com.queueless.dto.ApiResponse;
import com.queueless.dto.QueuePredictionDto;
import com.queueless.dto.ServiceDto;
import com.queueless.service.OrganizationService;
import com.queueless.service.QueuePredictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
@Tag(name = "Services", description = "Endpoints for service management and queue predictions")
public class ServiceController {

    private final OrganizationService organizationService;
    private final QueuePredictionService queuePredictionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create service (Admin)")
    public ResponseEntity<ApiResponse<ServiceDto>> createService(@Valid @RequestBody ServiceDto dto) {
        return ResponseEntity.ok(ApiResponse.success(organizationService.createService(dto), "Service created"));
    }

    @GetMapping("/{id}/prediction")
    @Operation(summary = "Get crowd and waiting time prediction for a service")
    public ResponseEntity<ApiResponse<QueuePredictionDto>> getServicePrediction(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(queuePredictionService.getPredictionForService(id)));
    }
}
