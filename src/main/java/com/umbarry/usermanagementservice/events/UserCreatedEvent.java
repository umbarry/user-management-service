package com.umbarry.usermanagementservice.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserCreatedEvent implements Serializable {
    private Long userId;
    private String email;
    private String username;
    private String name;
    private String surname;
    private String password;
}
