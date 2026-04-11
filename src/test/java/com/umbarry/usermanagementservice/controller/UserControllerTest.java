package com.umbarry.usermanagementservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umbarry.usermanagementservice.dto.UpdateStatusRequest;
import com.umbarry.usermanagementservice.dto.UserRequest;
import com.umbarry.usermanagementservice.enumeration.Role;
import com.umbarry.usermanagementservice.model.User;
import com.umbarry.usermanagementservice.enumeration.UserStatus;
import com.umbarry.usermanagementservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void createUser_shouldReturnCreatedUser() throws Exception {
        UserRequest request = UserRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .taxCode("RSSMRA80A01H501U")
                .name("Mario")
                .surname("Rossi")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.DEVELOPER))
                .build();

        mockMvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void getUserById_shouldReturnUser() throws Exception {
        User user = userRepository.save(User.builder()
                .username("testuser")
                .email("test@example.com")
                .taxCode("RSSMRA80A01H501U")
                .name("Mario")
                .surname("Rossi")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.DEVELOPER))
                .build());

        mockMvc.perform(get("/v1/users/" + user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void updateUser_shouldReturnUpdatedUser() throws Exception {
        User user = userRepository.save(User.builder()
                .username("olduser")
                .email("test@example.com")
                .taxCode("RSSMRA80A01H501U")
                .name("Mario")
                .surname("Rossi")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.DEVELOPER))
                .build());

        UserRequest request = UserRequest.builder()
                .username("newuser")
                .email("test@example.com")
                .taxCode("RSSMRA80A01H501U")
                .name("Mario")
                .surname("Rossi")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.DEVELOPER))
                .build();

        mockMvc.perform(put("/v1/users/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    void updateUserStatus_shouldReturnNoContent() throws Exception {
        User user = userRepository.save(User.builder()
                .username("testuser")
                .email("test@example.com")
                .taxCode("RSSMRA80A01H501U")
                .name("Mario")
                .surname("Rossi")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.DEVELOPER))
                .build());

        UpdateStatusRequest request = UpdateStatusRequest.builder().status(UserStatus.DISABLED).build();

        mockMvc.perform(put("/v1/users/" + user.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        // Verify status updated
        mockMvc.perform(get("/v1/users/" + user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    @Test
    void deleteUser_shouldReturnNoContent() throws Exception {
        User user = userRepository.save(User.builder()
                .username("testuser")
                .email("test@example.com")
                .taxCode("RSSMRA80A01H501U")
                .name("Mario")
                .surname("Rossi")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.DEVELOPER))
                .build());

        mockMvc.perform(delete("/v1/users/" + user.getId()))
                .andExpect(status().isNoContent());

        // Verify status is DELETED
        mockMvc.perform(get("/v1/users/" + user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELETED"));
    }

    @Test
    void createUser_duplicateEmail_returnsConflict() throws Exception {
        userRepository.save(User.builder()
                .username("user1")
                .email("duplicate@example.com")
                .taxCode("RSSMRA80A01H501U")
                .name("Mario")
                .surname("Rossi")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.DEVELOPER))
                .build());

        UserRequest request = UserRequest.builder()
                .username("user2")
                .email("duplicate@example.com")
                .taxCode("RSSMRA80A01H502V")
                .name("Luigi")
                .surname("Verdi")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.DEVELOPER))
                .build();

        mockMvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
