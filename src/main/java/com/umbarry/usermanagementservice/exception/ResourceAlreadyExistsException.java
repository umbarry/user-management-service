package com.umbarry.usermanagementservice.exception;

import lombok.Getter;

@Getter
public class ResourceAlreadyExistsException extends RuntimeException {
    private final ResourceType resourceType;
    private final String field;
    private final String value;

    public ResourceAlreadyExistsException(ResourceType resourceType, String field, String value) {
        super(String.format("%s with %s '%s' already exists", resourceType.getDisplayName(), field, value));
        this.resourceType = resourceType;
        this.field = field;
        this.value = value;
    }
}
