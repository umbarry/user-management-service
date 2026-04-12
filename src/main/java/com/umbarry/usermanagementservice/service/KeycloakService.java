package com.umbarry.usermanagementservice.service;

import com.umbarry.usermanagementservice.dto.UserRequest;
import com.umbarry.usermanagementservice.enumeration.Role;

import java.util.Set;

public interface KeycloakService {
    String createUser(UserRequest request, String password);
    void updateUserRoles(String email, Set<Role> roles);
    void updateUserStatus(String email, boolean enabled);
    void deleteUser(String email);
}
