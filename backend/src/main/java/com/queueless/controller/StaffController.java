package com.queueless.controller;

import com.queueless.dto.ApiResponse;
import com.queueless.dto.QueueTokenDto;
import com.queueless.security.UserPrincipal;
import com.queueless.service.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Staff Queue Operations", description = "Endpoints for staff counter and queue actions")
public class StaffController {

    private final QueueService queueService;

    @GetMapping("/counters/{counterId}/waiting-tokens")
    @Operation(summary = "Get list of waiting queue tokens for counter")
    public ResponseEntity<ApiResponse<List<QueueTokenDto>>> getWaitingTokensForCounter(@PathVariable Long counterId) {
        return ResponseEntity.ok(ApiResponse.success(queueService.getWaitingTokensForCounter(counterId)));
    }

    @PostMapping("/counters/{counterId}/call-next")
    @Operation(summary = "Call next waiting token to counter")
    public ResponseEntity<ApiResponse<QueueTokenDto>> callNextToken(
            @PathVariable Long counterId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        QueueTokenDto dto = queueService.callNextToken(counterId, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(dto, "Token called successfully"));
    }

    @PostMapping("/tokens/{tokenId}/start-service")
    @Operation(summary = "Start serving token")
    public ResponseEntity<ApiResponse<QueueTokenDto>> startService(
            @PathVariable Long tokenId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        QueueTokenDto dto = queueService.startService(tokenId, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(dto, "Service started"));
    }

    @PostMapping("/tokens/{tokenId}/complete")
    @Operation(summary = "Complete token service")
    public ResponseEntity<ApiResponse<QueueTokenDto>> completeToken(
            @PathVariable Long tokenId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        QueueTokenDto dto = queueService.completeToken(tokenId, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(dto, "Token completed successfully"));
    }

    @PostMapping("/tokens/{tokenId}/skip")
    @Operation(summary = "Skip token")
    public ResponseEntity<ApiResponse<QueueTokenDto>> skipToken(@PathVariable Long tokenId) {
        QueueTokenDto dto = queueService.skipToken(tokenId);
        return ResponseEntity.ok(ApiResponse.success(dto, "Token skipped"));
    }

    @PostMapping("/tokens/{tokenId}/recall")
    @Operation(summary = "Recall skipped token back to waiting queue")
    public ResponseEntity<ApiResponse<QueueTokenDto>> recallSkippedToken(@PathVariable Long tokenId) {
        QueueTokenDto dto = queueService.recallSkippedToken(tokenId);
        return ResponseEntity.ok(ApiResponse.success(dto, "Token recalled to waiting status"));
    }

    @PostMapping("/tokens/{tokenId}/no-show")
    @Operation(summary = "Mark customer as no-show")
    public ResponseEntity<ApiResponse<QueueTokenDto>> markNoShow(@PathVariable Long tokenId) {
        QueueTokenDto dto = queueService.markNoShow(tokenId);
        return ResponseEntity.ok(ApiResponse.success(dto, "Token marked as NO_SHOW"));
    }
}
