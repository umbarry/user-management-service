package com.umbarry.usermanagementservice.exception;

import lombok.Getter;

@Getter
public enum ResourceType {
    USER("User");

    private final String displayName;

    ResourceType(String displayName) {
        this.displayName = displayName;
    }

}
