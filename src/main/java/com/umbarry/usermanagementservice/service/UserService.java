package com.umbarry.usermanagementservice.service;

import com.umbarry.usermanagementservice.dto.UpdateStatusRequest;
import com.umbarry.usermanagementservice.dto.UserRequest;
import com.umbarry.usermanagementservice.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    Page<UserResponse> getAllUsers(Pageable pageable);
    UserResponse getUserById(Long id);
    UserResponse createUser(UserRequest request);
    UserResponse updateUser(Long id, UserRequest request);
    void updateUserStatus(Long id, UpdateStatusRequest status);
    void deleteUser(Long id);
}
