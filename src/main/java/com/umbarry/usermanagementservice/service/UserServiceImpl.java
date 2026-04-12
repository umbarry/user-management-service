package com.umbarry.usermanagementservice.service;

import com.umbarry.usermanagementservice.config.RabbitMQConfig;
import com.umbarry.usermanagementservice.dto.UpdateStatusRequest;
import com.umbarry.usermanagementservice.events.UserCreatedEvent;
import com.umbarry.usermanagementservice.dto.UserRequest;
import com.umbarry.usermanagementservice.dto.UserResponse;
import com.umbarry.usermanagementservice.exception.ResourceAlreadyExistsException;
import com.umbarry.usermanagementservice.exception.ResourceNotFoundException;
import com.umbarry.usermanagementservice.exception.ResourceType;
import com.umbarry.usermanagementservice.model.User;
import com.umbarry.usermanagementservice.enumeration.UserStatus;
import com.umbarry.usermanagementservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;
    private final KeycloakService keycloakService;

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        log.info("Retrieving all users - Page: {}, Size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return userRepository.findByStatusIn(List.of(UserStatus.ACTIVE, UserStatus.DISABLED), pageable).map(UserResponse::fromUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        log.info("Retrieving user with ID: {}", id);
        return userRepository.findById(id)
                .map(UserResponse::fromUser)
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", id);
                    return new ResourceNotFoundException(ResourceType.USER, id);
                });
    }

    @Override
    @Transactional
    public UserResponse createUser(UserRequest request) {
        log.info("Creating user: {}", request.getUsername());
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Email already exists: {}", request.getEmail());
            throw new ResourceAlreadyExistsException(ResourceType.USER, "email", request.getEmail());
        }
        if (userRepository.findByTaxCode(request.getTaxCode()).isPresent()) {
            log.warn("Tax code already exists: {}", request.getTaxCode());
            throw new ResourceAlreadyExistsException(ResourceType.USER, "taxCode", request.getTaxCode());
        }

        String password = UUID.randomUUID().toString().substring(0, 8);
        keycloakService.createUser(request, password);

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .taxCode(request.getTaxCode())
                .name(request.getName())
                .surname(request.getSurname())
                .status(request.getStatus())
                .roles(request.getRoles())
                .build();

        User savedUser = userRepository.save(user);
        log.debug("User created in DB with ID: {}", savedUser.getId());

        UserCreatedEvent event = UserCreatedEvent.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .username(savedUser.getUsername())
                .name(savedUser.getName())
                .surname(savedUser.getSurname())
                .password(password)
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.USER_EXCHANGE, RabbitMQConfig.USER_CREATED_ROUTING_KEY, event);
        log.info("Sent user created event for user: {}", savedUser.getUsername());

        return UserResponse.fromUser(savedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserRequest request) {
        log.info("Updating user with ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ResourceType.USER, id));

        if (!user.getEmail().equals(request.getEmail())) {
            throw new IllegalArgumentException("Email is not editable");
        }

        // Update Keycloak roles if changed
        if (!user.getRoles().equals(request.getRoles())) {
            keycloakService.updateUserRoles(user.getEmail(), request.getRoles());
        }

        user.setUsername(request.getUsername());
        user.setName(request.getName());
        user.setSurname(request.getSurname());
        user.setRoles(request.getRoles());

        User updatedUser = userRepository.save(user);
        return UserResponse.fromUser(updatedUser);
    }

    @Override
    @Transactional
    public void updateUserStatus(Long id, UpdateStatusRequest request) {
        log.info("Updating status for user ID: {} to {}", id, request.getStatus());

        // can only set active or disabled
        if (!request.getStatus().equals(UserStatus.ACTIVE) && !request.getStatus().equals(UserStatus.DISABLED)) {
            throw new IllegalArgumentException("Invalid status: " + request.getStatus());
        }

        // retrieve
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ResourceType.USER, id));

        // can only update id not DELETED
        if (user.getStatus().equals(UserStatus.DELETED)) {
            log.warn("User with ID {} is already marked as deleted", id);
            throw new IllegalStateException("Cannot update status of a deleted user");
        }

        boolean isEnabled = request.getStatus().equals(UserStatus.ACTIVE);
        keycloakService.updateUserStatus(user.getEmail(), isEnabled);

        user.setStatus(request.getStatus());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        log.info("Deleting user with ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ResourceType.USER, id));

        if(user.getStatus().equals(UserStatus.DELETED)) {
            log.warn("User with ID {} is already marked as deleted", id);
            return;
        }

        keycloakService.deleteUser(user.getEmail());

        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);
    }
}
