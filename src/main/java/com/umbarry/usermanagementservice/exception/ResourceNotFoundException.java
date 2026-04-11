package com.umbarry.usermanagementservice.exception;

public class ResourceNotFoundException extends RuntimeException {
    private final ResourceType resourceType;
    private final Long resourceId;

    public ResourceNotFoundException(ResourceType resourceType, Long resourceId) {
        super(String.format("%s not found with id: %d", resourceType.getDisplayName(), resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public ResourceNotFoundException(ResourceType resourceType, String message) {
        super(message);
        this.resourceType = resourceType;
        this.resourceId = null;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public Long getResourceId() {
        return resourceId;
    }
}
