package com.umbarry.usermanagementservice.controller;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umbarry.usermanagementservice.dto.UpdateStatusRequest;
import com.umbarry.usermanagementservice.dto.UserRequest;
import com.umbarry.usermanagementservice.dto.UserResponse;
import com.umbarry.usermanagementservice.dto.Views;
import com.umbarry.usermanagementservice.service.UserService;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ObjectMapper objectMapper;

    @ApiResponse(
            responseCode = "200",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserResponse.class))),
            headers = {
                @Header(name = "X-Total-Count", description = "Total number of elements", schema = @Schema(type = "integer")),
                @Header(name = "X-Total-Pages", description = "Total number of pages", schema = @Schema(type = "integer")),
                @Header(name = "X-Page-Number", description = "Current page number", schema = @Schema(type = "integer")),
                @Header(name = "X-Page-Size", description = "Page size", schema = @Schema(type = "integer"))
            }
    )
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'OPERATOR', 'MAINTAINER', 'DEVELOPER', 'REPORTER')")
    public ResponseEntity<String> getAllUsers(Pageable pageable, Authentication authentication) throws JsonProcessingException {
        Page<UserResponse> page = userService.getAllUsers(pageable);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(page.getTotalElements()));
        headers.add("X-Total-Pages", String.valueOf(page.getTotalPages()));
        headers.add("X-Page-Number", String.valueOf(page.getNumber()));
        headers.add("X-Page-Size", String.valueOf(page.getSize()));

        String json = objectMapper.writerWithView(Views.getViewForRole(authentication.getAuthorities())).writeValueAsString(page.getContent());

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = UserResponse.class))
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'OPERATOR', 'MAINTAINER', 'DEVELOPER', 'REPORTER')")
    public ResponseEntity<String> getUserById(@PathVariable Long id, Authentication authentication) throws JsonProcessingException {
        UserResponse userResponse = userService.getUserById(id);
        String json = objectMapper.writerWithView(Views.getViewForRole(authentication.getAuthorities())).writeValueAsString(userResponse);

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    @ApiResponse(
        responseCode = "201",
        content = @Content(schema = @Schema(implementation = UserResponse.class))
    )
    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    @JsonView(Views.Developer.class)
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = UserResponse.class))
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    @JsonView(Views.Developer.class)
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    public ResponseEntity<Void> updateUserStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest request) {
        userService.updateUserStatus(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
