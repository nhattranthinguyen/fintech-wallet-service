package com.nhattranthinguyen.wallet.wallet.api;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(name = "CreateWalletRequest", description = "Data required to create a wallet")
public record CreateWalletRequest(
    @Schema(description = "Owner identifier", example = "22222222-2222-2222-2222-222222222222")
    @NotNull(message = "ownerId is required")
    UUID ownerId,

    @Schema(description = "Three-letter currency code", example = "USD")
    @NotBlank(message = "currency is required")
    @Pattern(
        regexp = "^[A-Za-z]{3}$",
        message = "currency must contain exactly 3 letters"
    )
    String currency
) {
}
