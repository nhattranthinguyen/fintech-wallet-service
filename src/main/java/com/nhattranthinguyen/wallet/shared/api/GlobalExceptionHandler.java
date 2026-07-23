package com.nhattranthinguyen.wallet.shared.api;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.nhattranthinguyen.wallet.wallet.application.exception.WalletAlreadyExistsException;
import com.nhattranthinguyen.wallet.wallet.application.exception.WalletNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleWalletNotFound(
        WalletNotFoundException exception
    ) {
        return error(
            HttpStatus.NOT_FOUND,
            exception.getMessage()
        );
    }

    @ExceptionHandler(WalletAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleWalletAlreadyExists(
        WalletAlreadyExistsException exception
    ) {
        return error(
            HttpStatus.CONFLICT,
            exception.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
        MethodArgumentNotValidException exception
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .sorted((left, right) -> Integer.compare(
                validationPriority(left.getCode()),
                validationPriority(right.getCode())
            ))
            .forEach(fieldError -> errors.putIfAbsent(
                fieldError.getField(),
                fieldError.getDefaultMessage()
            ));

        Map<String, Object> body = errorBody(
            HttpStatus.BAD_REQUEST,
            "Validation failed"
        );
        body.put("errors", errors);

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException() {
        return error(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred"
        );
    }

    private ResponseEntity<Map<String, Object>> error(
        HttpStatus status,
        String message
    ) {
        return ResponseEntity
            .status(status)
            .body(errorBody(status, message));
    }

    private Map<String, Object> errorBody(
        HttpStatus status,
        String message
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now(ZoneOffset.UTC));
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);

        return body;
    }

    private int validationPriority(String validationCode) {
        return switch (validationCode) {
            case "NotNull", "NotBlank", "NotEmpty" -> 0;
            default -> 1;
        };
    }
}
