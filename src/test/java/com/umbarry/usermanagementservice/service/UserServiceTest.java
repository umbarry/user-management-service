package com.umbarry.usermanagementservice.service;

import com.umbarry.usermanagementservice.config.RabbitMQConfig;
import com.umbarry.usermanagementservice.dto.UpdateStatusRequest;
import com.umbarry.usermanagementservice.dto.UserRequest;
import com.umbarry.usermanagementservice.dto.UserResponse;
import com.umbarry.usermanagementservice.events.UserCreatedEvent;
import com.umbarry.usermanagementservice.exception.ResourceAlreadyExistsException;
import com.umbarry.usermanagementservice.exception.ResourceNotFoundException;
import com.umbarry.usermanagementservice.enumeration.Role;
import com.umbarry.usermanagementservice.model.User;
import com.umbarry.usermanagementservice.enumeration.UserStatus;
import com.umbarry.usermanagementservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private KeycloakService keycloakService;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserRequest userRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .taxCode("RSSMRA80A01H501U")
                .name("Mario")
                .surname("Rossi")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.DEVELOPER))
                .build();

        userRequest = UserRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .taxCode("RSSMRA80A01H501U")
                .name("Mario")
                .surname("Rossi")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.DEVELOPER))
                .build();
    }

    @Test
    void getAllUsers_shouldReturnPaginatedUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(user));
        when(userRepository.findByStatusIn(List.of(UserStatus.ACTIVE, UserStatus.DISABLED), pageable)).thenReturn(userPage);

        Page<UserResponse> result = userService.getAllUsers(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("testuser", result.getContent().get(0).getUsername());
    }

    @Test
    void getUserById_shouldReturnUser_whenUserExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void getUserById_shouldThrowException_whenUserDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(1L));
    }

    @Test
    void createUser_shouldCreateUserSuccessfully() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByTaxCode(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse result = userService.createUser(userRequest);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userRepository).save(any(User.class));

        verify(keycloakService, times(1)).createUser(any(UserRequest.class), anyString());
        verify(rabbitTemplate, times(1)).convertAndSend(eq(RabbitMQConfig.USER_EXCHANGE), eq(RabbitMQConfig.USER_CREATED_ROUTING_KEY), any(UserCreatedEvent.class));
    }

    @Test
    void createUser_shouldThrowException_whenEmailAlreadyExists() {
        when(userRepository.findByEmail(userRequest.getEmail())).thenReturn(Optional.of(user));

        assertThrows(ResourceAlreadyExistsException.class, () -> userService.createUser(userRequest));
    }

    @Test
    void updateUser_shouldUpdateUserSuccessfully() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse result = userService.updateUser(1L, userRequest);

        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_shouldThrowException_whenEmailIsModified() {
        userRequest.setEmail("newemail@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () -> userService.updateUser(1L, userRequest));
    }

    @Test
    void updateUserStatus_shouldUpdateStatusSuccessfully() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.updateUserStatus(1L, UpdateStatusRequest.builder().status(UserStatus.DISABLED).build());

        assertEquals(UserStatus.DISABLED, user.getStatus());
        verify(userRepository).save(user);
    }

    @Test
    void deleteUser_shouldMarkAsDeleted() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUser(1L);

        assertEquals(UserStatus.DELETED, user.getStatus());
        verify(userRepository).save(user);
    }
}
