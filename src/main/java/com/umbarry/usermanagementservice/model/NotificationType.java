package com.umbarry.usermanagementservice.model;

import lombok.Getter;

@Getter
public enum NotificationType {
    WELCOME_EMAIL("Welcome email");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }
}
