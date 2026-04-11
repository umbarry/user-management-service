package com.umbarry.usermanagementservice.dto;

import com.umbarry.usermanagementservice.enumeration.UserStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {

    @NotBlank(message = "Status is required")
    UserStatus status;
}
