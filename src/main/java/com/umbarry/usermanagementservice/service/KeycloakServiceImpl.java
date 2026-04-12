package com.umbarry.usermanagementservice.service;

import com.umbarry.usermanagementservice.dto.UserRequest;
import com.umbarry.usermanagementservice.enumeration.Role;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakServiceImpl implements KeycloakService {

    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    @Override
    public String createUser(UserRequest request, String password) {
        log.info("Creating user in Keycloak with email: {}", request.getEmail());

        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(request.getEmail());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getEmail());
        user.setLastName(request.getEmail());
        user.setEmailVerified(true);
        user.setRequiredActions(Collections.emptyList());

        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setTemporary(false);
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(password);

        user.setCredentials(Collections.singletonList(cred));

        UsersResource usersResource = keycloak.realm(realm).users();
        try (Response response = usersResource.create(user)) {
            if (response.getStatus() != 201) {
                log.error("Failed to create user in Keycloak. Status: {}", response.getStatus());
                throw new RuntimeException("Could not create user in Keycloak. Status: " + response.getStatus());
            }
            
            String locationHeader = response.getHeaderString("Location");
            String keycloakUserId = locationHeader.substring(locationHeader.lastIndexOf("/") + 1);
            
            // Assign roles
            assignRoles(keycloakUserId, request.getRoles());
            
            return keycloakUserId;
        }
    }

    @Override
    public void updateUserRoles(String email, Set<Role> roles) {
        String userId = getUserIdByEmail(email);
        
        // Remove existing roles
        UserResource userResource = keycloak.realm(realm).users().get(userId);
        List<RoleRepresentation> existingRoles = userResource.roles().realmLevel().listAll();
        userResource.roles().realmLevel().remove(existingRoles);
        
        // Assign new roles
        assignRoles(userId, roles);
    }

    @Override
    public void updateUserStatus(String email, boolean enabled) {
        String userId = getUserIdByEmail(email);
        UserRepresentation user = keycloak.realm(realm).users().get(userId).toRepresentation();
        user.setEnabled(enabled);
        keycloak.realm(realm).users().get(userId).update(user);
    }

    @Override
    public void deleteUser(String email) {
        String userId = getUserIdByEmail(email);
        keycloak.realm(realm).users().get(userId).remove();
    }

    private void assignRoles(String userId, Set<Role> roles) {
        List<RoleRepresentation> keycloakRoles = roles.stream()
                .map(role -> keycloak.realm(realm).roles().get(role.name()).toRepresentation())
                .collect(Collectors.toList());
        
        keycloak.realm(realm).users().get(userId).roles().realmLevel().add(keycloakRoles);
    }

    private String getUserIdByEmail(String email) {
        List<UserRepresentation> users = keycloak.realm(realm).users().searchByEmail(email, true);
        if (users.isEmpty()) {
            throw new RuntimeException("User not found in Keycloak with email: " + email);
        }
        return users.get(0).getId();
    }
}
