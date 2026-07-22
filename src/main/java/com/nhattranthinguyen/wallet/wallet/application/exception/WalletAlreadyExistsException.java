package com.nhattranthinguyen.wallet.wallet.application.exception;

import java.util.UUID;

public class WalletAlreadyExistsException extends RuntimeException {
    public WalletAlreadyExistsException(UUID ownerId, String currency) {
        super(
            "Wallet already exists for owner %s and currency %s"
                .formatted(ownerId, currency)
        );
    }
}