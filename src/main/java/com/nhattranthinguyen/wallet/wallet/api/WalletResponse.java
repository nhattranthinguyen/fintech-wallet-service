package com.nhattranthinguyen.wallet.wallet.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.nhattranthinguyen.wallet.wallet.domain.Wallet;

public record WalletResponse(
    UUID id,
    UUID ownerId,
    String currency,
    BigDecimal balance,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
            wallet.getId(),
            wallet.getOwnerId(),
            wallet.getCurrency(),
            wallet.getBalance(),
            wallet.getCreatedAt(),
            wallet.getUpdatedAt()
        );
    }
}
