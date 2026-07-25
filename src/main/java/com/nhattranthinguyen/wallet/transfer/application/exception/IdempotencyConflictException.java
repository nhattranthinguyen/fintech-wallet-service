package com.nhattranthinguyen.wallet.transfer.application.exception;

public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException() {
        super("The idempotency key has already been used with a different request.");
    }
}
