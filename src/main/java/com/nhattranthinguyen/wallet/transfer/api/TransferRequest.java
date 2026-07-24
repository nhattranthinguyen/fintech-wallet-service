package com.nhattranthinguyen.wallet.transfer.api;

import java.math.BigDecimal;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Schema(name = "TransferRequest", description = "Data required to transfer money")
public record TransferRequest(
        @Schema(description = "Wallet to debit", example = "11111111-1111-1111-1111-111111111111")
        @NotNull(message = "Source wallet ID is required")
        UUID sourceWalletId,

        @Schema(description = "Wallet to credit", example = "33333333-3333-3333-3333-333333333333")
        @NotNull(message = "Destination wallet ID is required")
        UUID destinationWalletId,

        @Schema(description = "Positive amount to transfer", example = "25.0000")
        @NotNull(message = "Amount is required")
        @DecimalMin(
                value = "0.00",
                inclusive = false,
                message = "Amount must be greater than zero")
        BigDecimal amount
) {
}
