package com.nhattranthinguyen.wallet.ledger.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Id;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {
    @Id
    private UUID id;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "transfer_id")
    private UUID transferId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    private LedgerEntryType entryType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected LedgerEntry() {
    }

    private LedgerEntry(
        UUID id,
        UUID walletId,
        UUID transferId,
        LedgerEntryType entryType,
        BigDecimal amount,
        BigDecimal balanceAfter,
        OffsetDateTime createdAt
    ) {
        this.id = id;
        this.walletId = walletId;
        this.transferId = transferId;
        this.entryType = entryType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.createdAt = createdAt;
    }

    public static LedgerEntry create(
        UUID walletId,
        UUID transferId,
        LedgerEntryType entryType,
        BigDecimal amount,
        BigDecimal balanceAfter
    ) {
        return new LedgerEntry(
            UUID.randomUUID(),
            walletId,
            transferId,
            entryType,
            amount,
            balanceAfter,
            OffsetDateTime.now(ZoneOffset.UTC)
        );
    }

    public static LedgerEntry createAt(
        UUID walletId,
        UUID transferId,
        LedgerEntryType entryType,
        BigDecimal amount,
        BigDecimal balanceAfter,
        OffsetDateTime createdAt
    ) {
        return new LedgerEntry(
            UUID.randomUUID(),
            walletId,
            transferId,
            entryType,
            amount,
            balanceAfter,
            createdAt
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public UUID getTransferId() {
        return transferId;
    }

    public LedgerEntryType getEntryType() {
        return entryType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
