package com.queueless.dto;

import com.queueless.entity.Role;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Long id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private Role role;
    private Long organizationId;
    private String organizationName;
    private String organizationCategory;
}
