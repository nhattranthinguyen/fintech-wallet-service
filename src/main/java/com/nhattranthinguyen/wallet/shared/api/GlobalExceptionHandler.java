package com.nhattranthinguyen.wallet.shared.api;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.nhattranthinguyen.wallet.transfer.application.exception.CurrencyMismatchException;
import com.nhattranthinguyen.wallet.transfer.application.exception.IdempotencyConflictException;
import com.nhattranthinguyen.wallet.transfer.application.exception.SameWalletTransferException;
import com.nhattranthinguyen.wallet.wallet.application.exception.InsufficientBalanceException;
import com.nhattranthinguyen.wallet.wallet.application.exception.WalletAlreadyExistsException;
import com.nhattranthinguyen.wallet.wallet.application.exception.WalletNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleWalletNotFound(
        WalletNotFoundException exception
    ) {
        return error(
            HttpStatus.NOT_FOUND,
            exception.getMessage()
        );
    }

    @ExceptionHandler(WalletAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleWalletAlreadyExists(
        WalletAlreadyExistsException exception
    ) {
        return error(
            HttpStatus.CONFLICT,
            exception.getMessage()
        );
    }

    @ExceptionHandler({
        InsufficientBalanceException.class,
        CurrencyMismatchException.class,
        SameWalletTransferException.class,
        IdempotencyConflictException.class
    })
    public ResponseEntity<ApiErrorResponse> handleTransferConflict(
        RuntimeException exception
    ) {
        return error(
            HttpStatus.CONFLICT,
            exception.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
        MethodArgumentNotValidException exception
    ) {
        Map<String, String> errors = new java.util.LinkedHashMap<>();

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

        return ResponseEntity.badRequest().body(errorBody(
            HttpStatus.BAD_REQUEST, "Validation failed", errors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException() {
        return error(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred"
        );
    }

    private ResponseEntity<ApiErrorResponse> error(
        HttpStatus status,
        String message
    ) {
        return ResponseEntity
            .status(status)
            .body(errorBody(status, message, null));
    }

    private ApiErrorResponse errorBody(
        HttpStatus status,
        String message,
        Map<String, String> errors
    ) {
        return new ApiErrorResponse(
            OffsetDateTime.now(ZoneOffset.UTC),
            status.value(),
            status.getReasonPhrase(),
            message,
            errors
        );
    }

    private int validationPriority(String validationCode) {
        return switch (validationCode) {
            case "NotNull", "NotBlank", "NotEmpty" -> 0;
            default -> 1;
        };
    }
}
