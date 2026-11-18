package com.queueless.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceDto {
    private Long id;
    @NotBlank(message = "Service name is required")
    private String name;
    private String description;
    @NotBlank(message = "Prefix is required")
    private String prefix;
    private Integer defaultEstimatedDurationMinutes;
    @NotNull(message = "Department ID is required")
    private Long departmentId;
    private String departmentName;
    private Long organizationId;
    private String organizationName;
    private Boolean active;
    private Long queueId;
    private Integer currentWaitingCount;
    private Integer estimatedWaitMinutes;
}
