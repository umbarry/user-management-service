package com.umbarry.usermanagementservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umbarry.usermanagementservice.dto.UpdateStatusRequest;
import com.umbarry.usermanagementservice.dto.UserRequest;
import com.umbarry.usermanagementservice.enumeration.Role;
import com.umbarry.usermanagementservice.enumeration.UserStatus;
import com.umbarry.usermanagementservice.model.User;
import com.umbarry.usermanagementservice.repository.UserRepository;
import com.umbarry.usermanagementservice.service.KeycloakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @MockitoBean
    private KeycloakService keycloakService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    private JwtRequestPostProcessor roleJwt(String role) {
        return jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_" + role))
                .jwt(j -> j
                        .claim("realm_access", Map.of("roles", List.of(role)))
                        .claim("email", role.toLowerCase() + "@test.com")
                        .claim("sub", "test-subject-" + role)
                );
    }

    private JwtRequestPostProcessor ownerJwt() {
        return roleJwt("OWNER");
    }

    private JwtRequestPostProcessor developerJwt() {
        return roleJwt("DEVELOPER");
    }

    @Test
    void createUser_shouldReturnCreatedUser() throws Exception {
        when(keycloakService.createUser(any(), anyString())).thenReturn("test-keycloak-id");

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
                        .with(ownerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void createUser_asDeveloper_shouldReturn403() throws Exception {
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
                        .with(developerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserById_shouldReturnUser() throws Exception {
        User user = userRepository.save(User.builder()
                .username("testuser")
                .email("testuser@example.com")
                .taxCode("RSSMRA80A01H501U")
                .name("Mario")
                .surname("Rossi")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.OWNER))
                .build());

        mockMvc.perform(get("/v1/users/" + user.getId())
                        .with(ownerJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void updateUser_shouldReturnUpdatedUser() throws Exception {
        User user = userRepository.save(User.builder()
                .username("olduser")
                .email("testuser@example.com")
                .taxCode("RSSMRA80A01H501U")
                .name("Mario")
                .surname("Rossi")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.OWNER))
                .build());

        UserRequest request = UserRequest.builder()
                .username("newuser")
                .email("testuser@example.com")
                .taxCode("RSSMRA80A01H501U")
                .name("Mario")
                .surname("Rossi")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.OWNER))
                .build();

        mockMvc.perform(put("/v1/users/" + user.getId())
                        .with(ownerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    void updateUserStatus_shouldReturnNoContent() throws Exception {
        User user = userRepository.save(User.builder()
                .username("testuser")
                .email("testuser@example.com")
                .taxCode("RSSMRA80A01H501U")
                .name("Mario")
                .surname("Rossi")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.OWNER))
                .build());

        UpdateStatusRequest request = UpdateStatusRequest.builder()
                .status(UserStatus.DISABLED)
                .build();

        mockMvc.perform(put("/v1/users/" + user.getId() + "/status")
                        .with(ownerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/users/" + user.getId())
                        .with(ownerJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    @Test
    void deleteUser_shouldReturnNoContent() throws Exception {
        User user = userRepository.save(User.builder()
                .username("testuser")
                .email("testuser@example.com")
                .taxCode("RSSMRA80A01H501U")
                .name("Mario")
                .surname("Rossi")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.OWNER))
                .build());

        mockMvc.perform(delete("/v1/users/" + user.getId())
                        .with(ownerJwt()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/users/" + user.getId())
                        .with(ownerJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELETED"));
    }

    @Test
    void createUser_duplicateEmail_returnsConflict() throws Exception {
        when(keycloakService.createUser(any(), anyString())).thenReturn("test-keycloak-id");

        userRepository.save(User.builder()
                .username("admin")
                .email("admin@example.com")
                .taxCode("ADMUSR80A01H501P")
                .name("Admin")
                .surname("User")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.OWNER))
                .build());

        UserRequest request = UserRequest.builder()
                .username("user2")
                .email("admin@example.com")
                .taxCode("RSSMRA80A01H502V")
                .name("Luigi")
                .surname("Verdi")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.OWNER))
                .build();

        mockMvc.perform(post("/v1/users")
                        .with(ownerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void noToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/v1/users/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reporterView_shouldOnlySeeAllowedFields() throws Exception {
        User testUser = userRepository.save(User.builder()
                .username("testvisibility")
                .email("visibility@example.com")
                .taxCode("VISTES88A01H501U")
                .name("Visibility")
                .surname("Test")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.OPERATOR))
                .build());

        mockMvc.perform(get("/v1/users/" + testUser.getId())
                        .with(roleJwt("REPORTER")))
                .andExpect(status().isOk())
                // Visible fields
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("testvisibility"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                // Hidden fields
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.taxCode").doesNotExist())
                .andExpect(jsonPath("$.name").doesNotExist())
                .andExpect(jsonPath("$.surname").doesNotExist())
                .andExpect(jsonPath("$.roles").doesNotExist());
    }

    @Test
    void operatorView_shouldSeeEverythingExceptRoles() throws Exception {
        User testUser = userRepository.save(User.builder()
                .username("testvisibility")
                .email("visibility@example.com")
                .taxCode("VISTES88A01H501U")
                .name("Visibility")
                .surname("Test")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.OPERATOR))
                .build());

        mockMvc.perform(get("/v1/users/" + testUser.getId())
                        .with(roleJwt("OPERATOR")))
                .andExpect(status().isOk())
                // Visible fields
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("testvisibility"))
                .andExpect(jsonPath("$.email").value("visibility@example.com"))
                .andExpect(jsonPath("$.taxCode").value("VISTES88A01H501U"))
                .andExpect(jsonPath("$.name").value("Visibility"))
                .andExpect(jsonPath("$.surname").value("Test"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                // Hidden fields
                .andExpect(jsonPath("$.roles").doesNotExist());
    }

    @Test
    void developerView_shouldSeeAllFields() throws Exception {
        User testUser = userRepository.save(User.builder()
                .username("testvisibility")
                .email("visibility@example.com")
                .taxCode("VISTES88A01H501U")
                .name("Visibility")
                .surname("Test")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.OPERATOR))
                .build());

        mockMvc.perform(get("/v1/users/" + testUser.getId())
                        .with(roleJwt("DEVELOPER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("testvisibility"))
                .andExpect(jsonPath("$.email").value("visibility@example.com"))
                .andExpect(jsonPath("$.roles").exists())
                .andExpect(jsonPath("$.roles[0]").value("OPERATOR"));
    }
}
