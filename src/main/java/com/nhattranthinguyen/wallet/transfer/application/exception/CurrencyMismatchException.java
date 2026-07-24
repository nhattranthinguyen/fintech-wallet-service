package com.nhattranthinguyen.wallet.transfer.application.exception;

public class CurrencyMismatchException extends RuntimeException {
    public CurrencyMismatchException() {
        super("Source and destination currencies must match.");
    }
}
