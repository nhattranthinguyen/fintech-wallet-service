package com.nhattranthinguyen.wallet.transfer.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.nhattranthinguyen.wallet.transfer.domain.Transfer;
import com.nhattranthinguyen.wallet.transfer.domain.TransferStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Transfer", description = "Completed transfer details")
public record TransferResponse(
        @Schema(example = "44444444-4444-4444-4444-444444444444")
        UUID id,
        @Schema(example = "11111111-1111-1111-1111-111111111111")
        UUID sourceWalletId,
        @Schema(example = "33333333-3333-3333-3333-333333333333")
        UUID destinationWalletId,
        @Schema(example = "25.0000")
        BigDecimal amount,
        @Schema(example = "USD")
        String currency,
        @Schema(example = "COMPLETED")
        TransferStatus status,
        @Schema(example = "2026-07-25T03:34:10Z")
        OffsetDateTime createdAt) {
    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getSourceWalletId(),
                transfer.getDestinationWalletId(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getStatus(),
                transfer.getCreatedAt());
    }
}
