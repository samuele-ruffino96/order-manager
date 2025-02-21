package com.company.app.ordermanager.exception.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@Getter
@Setter
public class ErrorResponseDto {
    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String path;
    private final String message;

    protected ErrorResponseDto(int status, String error, String message, String path) {
        this.timestamp = Instant.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    // Factory method for creating standard error responses
    public static ErrorResponseDto of(HttpStatus status, String message, String path) {
        return new ErrorResponseDto(
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
    }
}
