package com.nhattranthinguyen.wallet.wallet.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.nhattranthinguyen.wallet.wallet.domain.Wallet;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Wallet", description = "Wallet details and current balance")
public record WalletResponse(
    @Schema(example = "11111111-1111-1111-1111-111111111111")
    UUID id,
    @Schema(example = "22222222-2222-2222-2222-222222222222")
    UUID ownerId,
    @Schema(example = "USD")
    String currency,
    @Schema(example = "0.0000")
    BigDecimal balance,
    @Schema(example = "2026-07-25T03:34:10Z")
    OffsetDateTime createdAt,
    @Schema(example = "2026-07-25T03:34:10Z")
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
