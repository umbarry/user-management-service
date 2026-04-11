package com.umbarry.usermanagementservice.model;

import com.umbarry.usermanagementservice.enumeration.Role;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RoleConverter implements AttributeConverter<Role, Short> {

    @Override
    public Short convertToDatabaseColumn(Role role) {
        if (role == null) {
            return null;
        }
        return role.getValue();
    }

    @Override
    public Role convertToEntityAttribute(Short value) {
        if (value == null) {
            return null;
        }
        return Role.fromValue(value);
    }
}
