package com.queueless.controller;

import com.queueless.dto.*;
import com.queueless.security.UserPrincipal;
import com.queueless.service.OrganizationService;
import com.queueless.service.QrCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizations", description = "Endpoints for discovering and managing organizations")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final QrCodeService qrCodeService;

    @GetMapping
    @Operation(summary = "Search and list all active organizations")
    public ResponseEntity<ApiResponse<List<OrganizationDto>>> getAllOrganizations(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.success(organizationService.getAllOrganizations(category, search)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get organization by ID")
    public ResponseEntity<ApiResponse<OrganizationDto>> getOrganizationById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(organizationService.getOrganizationById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create a new organization (Admin)")
    public ResponseEntity<ApiResponse<OrganizationDto>> createOrganization(@Valid @RequestBody OrganizationDto dto) {
        return ResponseEntity.ok(ApiResponse.success(organizationService.createOrganization(dto), "Organization created"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update an organization (Admin)")
    public ResponseEntity<ApiResponse<OrganizationDto>> updateOrganization(
            @PathVariable Long id, @Valid @RequestBody OrganizationDto dto) {
        return ResponseEntity.ok(ApiResponse.success(organizationService.updateOrganization(id, dto), "Organization updated"));
    }

    @GetMapping("/{id}/departments")
    @Operation(summary = "Get departments of an organization")
    public ResponseEntity<ApiResponse<List<DepartmentDto>>> getDepartments(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(organizationService.getDepartments(id)));
    }

    @GetMapping("/{id}/services")
    @Operation(summary = "Get services of an organization")
    public ResponseEntity<ApiResponse<List<ServiceDto>>> getServices(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(organizationService.getServicesByOrganization(id)));
    }

    @GetMapping("/{id}/counters")
    @Operation(summary = "Get counters of an organization")
    public ResponseEntity<ApiResponse<List<CounterDto>>> getCounters(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(organizationService.getCountersByOrganization(id)));
    }

    @PostMapping("/{id}/initialize-counter")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Initialize a default counter for organization if none exists")
    public ResponseEntity<ApiResponse<CounterDto>> initializeDefaultCounter(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal != null ? userPrincipal.getId() : null;
        return ResponseEntity.ok(ApiResponse.success(organizationService.initializeDefaultCounter(id, userId), "Counter initialized"));
    }

    @GetMapping("/{id}/qr")
    @Operation(summary = "Generate QR code for organization digital queue access")
    public ResponseEntity<ApiResponse<String>> getOrganizationQrCode(@PathVariable Long id) {
        String qrContent = "https://queueless.app/org/" + id;
        String base64Qr = qrCodeService.generateQrCodeBase64(qrContent, 300, 300);
        return ResponseEntity.ok(ApiResponse.success(base64Qr, "QR Code generated"));
    }
}
