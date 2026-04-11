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

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        log.info("Retrieving all users - Page: {}, Size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return userRepository.findAll(pageable).map(UserResponse::fromUser);
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
        log.debug("User created with ID: {}", savedUser.getId());

        UserCreatedEvent event = UserCreatedEvent.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .username(savedUser.getUsername())
                .name(savedUser.getName())
                .surname(savedUser.getSurname())
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
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", id);
                    return new ResourceNotFoundException(ResourceType.USER, id);
                });

        if (!user.getEmail().equals(request.getEmail())) {
            log.warn("Attempted to edit email for user ID: {}", id);
            throw new IllegalArgumentException("Email is not editable");
        }

        if (!user.getTaxCode().equals(request.getTaxCode())) {
            if (userRepository.findByTaxCode(request.getTaxCode()).isPresent()) {
                log.warn("Tax code already exists: {}", request.getTaxCode());
                throw new ResourceAlreadyExistsException(ResourceType.USER, "taxCode", request.getTaxCode());
            }
            user.setTaxCode(request.getTaxCode());
        }

        // cannot update status
        if (!user.getStatus().equals(request.getStatus())) {
            log.warn("Attempted to edit status for user ID: {}", id);
            throw new IllegalArgumentException("Status is not editable. use updateUserStatus instead");
        }

        user.setUsername(request.getUsername());
        user.setName(request.getName());
        user.setSurname(request.getSurname());
        user.setStatus(request.getStatus());
        user.setRoles(request.getRoles());

        User updatedUser = userRepository.save(user);
        log.debug("User updated - ID: {}", updatedUser.getId());
        return UserResponse.fromUser(updatedUser);
    }

    @Override
    @Transactional
    public void updateUserStatus(Long id, UpdateStatusRequest request) {
        log.info("Updating status for user ID: {} to {}", id, request.getStatus());

        // can only set active or disabled status
        if (!request.getStatus().equals(UserStatus.ACTIVE) && !request.getStatus().equals(UserStatus.DISABLED)) {
            log.warn("Invalid status for user ID: {}", id);
            throw new IllegalArgumentException("User can only be enabled/disabled. For deletion use DELETE method.");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", id);
                    return new ResourceNotFoundException(ResourceType.USER, id);
                });
        user.setStatus(request.getStatus());
        userRepository.save(user);
        log.debug("User status updated - ID: {}", id);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        log.info("Deleting user with ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", id);
                    return new ResourceNotFoundException(ResourceType.USER, id);
                });
        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);
        log.debug("User marked as deleted - ID: {}", id);
    }
}
