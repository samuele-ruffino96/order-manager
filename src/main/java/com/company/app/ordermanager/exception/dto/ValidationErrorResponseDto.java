package com.company.app.ordermanager.exception.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
@Setter
public class ValidationErrorResponseDto extends ErrorResponseDto {
    private final List<FieldError> errors;

    private ValidationErrorResponseDto(String path, List<FieldError> errors) {
        super(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed",
                path
        );
        this.errors = errors;
    }

    public static ValidationErrorResponseDto of(String path, List<org.springframework.validation.FieldError> fieldErrors) {
        List<FieldError> errors = fieldErrors.stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return new ValidationErrorResponseDto(path, errors);
    }

    public record FieldError(String field, String message) {
    }
}
