package com.nhattranthinguyen.wallet.transfer.api;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record TransferRequest(
    @NotNull(message = "Source wallet ID is required")
        UUID sourceWalletId,

        @NotNull(message = "Destination wallet ID is required")
        UUID destinationWalletId,

        @NotNull(message = "Amount is required")
        @DecimalMin(
                value = "0.00",
                inclusive = false,
                message = "Amount must be greater than zero")
        BigDecimal amount
) {
}
