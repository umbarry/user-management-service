package com.umbarry.usermanagementservice.model;

import com.umbarry.usermanagementservice.enumeration.Role;
import com.umbarry.usermanagementservice.enumeration.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class User extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Username is required")
    private String username;

    @Column(nullable = false, unique = true, updatable = false)
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Tax code is required")
    @Pattern(regexp = "^[A-Z]{6}\\d{2}[A-Z]\\d{2}[A-Z]\\d{3}[A-Z]$", message = "Invalid Italian tax code format")
    private String taxCode;

    @Column(nullable = false)
    @NotBlank(message = "Name is required")
    private String name;

    @Column(nullable = false)
    @NotBlank(message = "Surname is required")
    private String surname;

    @Column(nullable = false, columnDefinition = "SMALLINT")
    @NotNull(message = "Status is required")
    @Convert(converter = UserStatusConverter.class)
    private UserStatus status;

    @ElementCollection(targetClass = Role.class)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Convert(converter = RoleConverter.class)
    @Column(name = "role", columnDefinition = "SMALLINT")
    private Set<Role> roles;
}
