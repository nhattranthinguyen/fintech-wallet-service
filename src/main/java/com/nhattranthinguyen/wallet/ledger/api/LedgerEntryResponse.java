package com.nhattranthinguyen.wallet.ledger.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.nhattranthinguyen.wallet.ledger.domain.LedgerEntry;
import com.nhattranthinguyen.wallet.ledger.domain.LedgerEntryType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LedgerEntry", description = "A wallet debit or credit recorded by a transfer")
public record LedgerEntryResponse(
        @Schema(example = "55555555-5555-5555-5555-555555555555") UUID id,
        @Schema(example = "44444444-4444-4444-4444-444444444444") UUID transferId,
        @Schema(example = "DEBIT") LedgerEntryType type,
        @Schema(example = "25.0000") BigDecimal amount,
        @Schema(example = "75.0000") BigDecimal balanceAfter,
        @Schema(example = "2026-07-25T03:34:10Z") OffsetDateTime createdAt
) {
    public static LedgerEntryResponse from(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.getId(),
                entry.getTransferId(),
                entry.getEntryType(),
                entry.getAmount(),
                entry.getBalanceAfter(),
                entry.getCreatedAt());
    }
}
