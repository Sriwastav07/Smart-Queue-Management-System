package com.queueless.controller;

import com.queueless.dto.ApiResponse;
import com.queueless.dto.IssueTokenRequest;
import com.queueless.dto.QueueTokenDto;
import com.queueless.security.UserPrincipal;
import com.queueless.service.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tokens")
@RequiredArgsConstructor
@Tag(name = "Tokens", description = "Endpoints for user queue token generation and tracking")
public class TokenController {

    private final QueueService queueService;

    @PostMapping
    @Operation(summary = "Issue a new queue token for a service")
    public ResponseEntity<ApiResponse<QueueTokenDto>> issueToken(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody IssueTokenRequest request) {
        QueueTokenDto dto = queueService.issueToken(userPrincipal.getId(), request.getServiceId(), request.getNotifyThreshold());
        return ResponseEntity.ok(ApiResponse.success(dto, "Token issued successfully"));
    }

    @GetMapping("/active")
    @Operation(summary = "Get user's current active queue token")
    public ResponseEntity<ApiResponse<QueueTokenDto>> getActiveToken(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        QueueTokenDto dto = queueService.getActiveTokenForUser(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/history")
    @Operation(summary = "Get user's queue token history")
    public ResponseEntity<ApiResponse<List<QueueTokenDto>>> getTokenHistory(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(ApiResponse.success(queueService.getUserTokenHistory(userPrincipal.getId())));
    }

    @PostMapping("/{tokenId}/cancel")
    @Operation(summary = "Cancel an active token")
    public ResponseEntity<ApiResponse<QueueTokenDto>> cancelToken(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long tokenId) {
        QueueTokenDto dto = queueService.cancelToken(tokenId, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(dto, "Token cancelled successfully"));
    }

    @PutMapping("/{tokenId}/leave-return")
    @Operation(summary = "Set leave & return notification threshold")
    public ResponseEntity<ApiResponse<QueueTokenDto>> setLeaveAndReturnThreshold(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long tokenId,
            @RequestParam Integer threshold) {
        QueueTokenDto dto = queueService.setLeaveAndReturnThreshold(tokenId, userPrincipal.getId(), threshold);
        return ResponseEntity.ok(ApiResponse.success(dto, "Notification threshold updated"));
    }
}
