package com.fooddelivery.user.dto;

import com.fooddelivery.user.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String email;
    private String phone;
    private String fullName;
    private UserRole role;
    private boolean active;
}
