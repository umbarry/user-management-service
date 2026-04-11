package com.umbarry.usermanagementservice.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum Role {
    OWNER((short) 1),
    OPERATOR((short) 2),
    MAINTAINER((short) 3),
    DEVELOPER((short) 4),
    REPORTER((short) 5);

    private final short value;

    public static Role fromValue(short value) {
        return Arrays.stream(Role.values())
                .filter(role -> role.getValue() == value)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown role value: " + value));
    }
}
