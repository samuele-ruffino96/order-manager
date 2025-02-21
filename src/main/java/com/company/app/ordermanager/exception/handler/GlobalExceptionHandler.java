package com.company.app.ordermanager.exception.handler;

import com.company.app.ordermanager.exception.dto.ErrorResponseDto;
import com.company.app.ordermanager.exception.dto.ValidationErrorResponseDto;
import com.company.app.ordermanager.exception.order.OrderNotFoundException;
import com.company.app.ordermanager.exception.product.ProductNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponseDto handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        return ValidationErrorResponseDto.of(
                request.getRequestURI(),
                ex.getBindingResult().getFieldErrors()
        );
    }

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponseDto handleProductNotFound(ProductNotFoundException ex, HttpServletRequest request) {
        return ErrorResponseDto.of(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponseDto handleOrderNotFound(OrderNotFoundException ex, HttpServletRequest request) {
        return ErrorResponseDto.of(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponseDto handleAllUncaughtException(Exception ex, HttpServletRequest request) {
        log.error("An unexpected error occurred: {}", ex.getMessage());

        return ErrorResponseDto.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                request.getRequestURI()
        );
    }
}
