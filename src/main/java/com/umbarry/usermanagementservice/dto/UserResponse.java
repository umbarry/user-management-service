package com.umbarry.usermanagementservice.dto;

import com.umbarry.usermanagementservice.enumeration.Role;
import com.umbarry.usermanagementservice.model.User;
import com.umbarry.usermanagementservice.enumeration.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String taxCode;
    private String name;
    private String surname;
    private UserStatus status;
    private Set<Role> roles;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    public static UserResponse fromUser(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .taxCode(user.getTaxCode())
                .name(user.getName())
                .surname(user.getSurname())
                .status(user.getStatus())
                .roles(user.getRoles())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
