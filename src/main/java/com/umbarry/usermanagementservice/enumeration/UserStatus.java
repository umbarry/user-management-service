package com.umbarry.usermanagementservice.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum UserStatus {
    ACTIVE((short) 1),
    DISABLED((short) 2),
    DELETED((short) 3);

    private final short value;

    public static UserStatus fromValue(short value) {
        return Arrays.stream(UserStatus.values())
                .filter(status -> status.getValue() == value)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown role value: " + value));
    }
}
