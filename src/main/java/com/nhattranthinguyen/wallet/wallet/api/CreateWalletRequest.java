package com.nhattranthinguyen.wallet.wallet.api;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateWalletRequest(
    @NotNull(message = "ownerId is required")
    UUID ownerId,

    @NotBlank(message = "currency is required")
    @Pattern(
        regexp = "^[A-Za-z]{3}$",
        message = "currency must contain exactly 3 letters"
    )
    String currency
) {
}
