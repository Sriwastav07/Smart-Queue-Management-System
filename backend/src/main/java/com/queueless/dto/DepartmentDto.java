package com.queueless.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentDto {
    private Long id;
    @NotBlank(message = "Department name is required")
    private String name;
    private String description;
    @NotNull(message = "Organization ID is required")
    private Long organizationId;
    private String organizationName;
}
