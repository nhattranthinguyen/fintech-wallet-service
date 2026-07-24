package com.nhattranthinguyen.wallet.transfer.application.exception;

public class SameWalletTransferException extends RuntimeException {
    public SameWalletTransferException() {
        super(
                "Source and destination wallets must be different.");
    }
}
