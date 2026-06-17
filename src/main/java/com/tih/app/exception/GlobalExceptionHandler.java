package com.tih.app.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.tih.app.util.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String SOURCE = "tih-app";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.RESOURCE_NOT_FOUND)
                .message(ex.getMessage())
                .source(SOURCE)
                .errors(List.of())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException ex) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.DUPLICATE_RESOURCE)
                .message(ex.getMessage())
                .source(SOURCE)
                .errors(List.of())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<ValidationError> validationErrors = ex.getBindingResult().getAllErrors().stream()
                .filter(FieldError.class::isInstance)
                .map(error -> (FieldError) error)
                .map(fieldError -> new ValidationError("VALIDATION_ERROR", fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();
        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.VALIDATION_FAILED)
                .message("Validation failed")
                .source(SOURCE)
                .errors(validationErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(UnknownLanguageException.class)
    public ResponseEntity<ErrorResponse> handleUnknownLanguage(UnknownLanguageException ex) {
        log.warn("Unknown language: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.UNKNOWN_LANGUAGE)
                .message(ex.getMessage())
                .source(SOURCE)
                .errors(List.of(new ValidationError(ErrorCode.UNKNOWN_LANGUAGE, "language", ex.getMessage())))
                .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid request: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.INVALID_REQUEST)
                .message(ex.getMessage())
                .source(SOURCE)
                .errors(List.of(new ValidationError(ErrorCode.INVALID_REQUEST, "request", ex.getMessage())))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.INTERNAL_SERVER_ERROR)
                .message("An unexpected error occurred")
                .source(SOURCE)
                .errors(List.of())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
