package com.umbarry.usermanagementservice.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String message;
    private String error;
    private LocalDateTime timestamp;

    public ErrorResponse(String message, String error) {
        this.message = message;
        this.error = error;
        this.timestamp = LocalDateTime.now();
    }
}

