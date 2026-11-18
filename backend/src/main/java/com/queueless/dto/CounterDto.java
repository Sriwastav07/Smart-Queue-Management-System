package com.queueless.dto;

import com.queueless.entity.CounterStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CounterDto {
    private Long id;
    @NotBlank(message = "Counter name is required")
    private String name;
    private String counterNumber;
    private Long departmentId;
    private String departmentName;
    private Long organizationId;
    private CounterStatus status;
    private Long assignedStaffId;
    private String assignedStaffName;
    private QueueTokenDto currentToken;
}
