package com.umbarry.usermanagementservice.dto;

import com.fasterxml.jackson.annotation.JsonView;
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
    @JsonView(Views.Reporter.class)
    private Long id;

    @JsonView(Views.Reporter.class)
    private String username;

    @JsonView(Views.Operator.class)
    private String email;

    @JsonView(Views.Operator.class)
    private String taxCode;

    @JsonView(Views.Operator.class)
    private String name;

    @JsonView(Views.Operator.class)
    private String surname;

    @JsonView(Views.Reporter.class)
    private UserStatus status;

    @JsonView(Views.Developer.class)
    private Set<Role> roles;

    @JsonView(Views.Reporter.class)
    private ZonedDateTime createdAt;

    @JsonView(Views.Reporter.class)
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
