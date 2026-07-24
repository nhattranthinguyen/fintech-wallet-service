package com.nhattranthinguyen.wallet.wallet.application.exception;

public class InvalidTransferAmountException extends RuntimeException {
    public InvalidTransferAmountException() {
        super("Transfer amount must be greater than zero.");
    }
}
