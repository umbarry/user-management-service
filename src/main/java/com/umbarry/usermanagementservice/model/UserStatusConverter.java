package com.umbarry.usermanagementservice.model;

import com.umbarry.usermanagementservice.enumeration.UserStatus;
import jakarta.persistence.AttributeConverter;

public class UserStatusConverter  implements AttributeConverter<UserStatus, Short> {

    @Override
    public Short convertToDatabaseColumn(UserStatus status) {
        if (status == null) {
            return null;
        }
        return status.getValue();
    }

    @Override
    public UserStatus convertToEntityAttribute(Short value) {
        if (value == null) {
            return null;
        }
        return UserStatus.fromValue(value);
    }
}
