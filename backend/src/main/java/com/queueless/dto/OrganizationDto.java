package com.queueless.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationDto {
    private Long id;
    @NotBlank(message = "Organization name is required")
    private String name;
    private String category;
    private String description;
    private String address;
    private String city;
    private String contactEmail;
    private String contactPhone;
    private Boolean active;
    private Integer totalDepartmentsCount;
    private Integer activeQueueCount;
    private Integer estimatedWaitMinutes;
    private LocalDateTime createdAt;
}
