package com.nhattranthinguyen.wallet.transfer.domain;

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
@Table(name = "transfers")
public class Transfer {
    @Id
    private UUID id;

    @Column(name = "source_wallet_id", nullable = false)
    private UUID sourceWalletId;

    @Column(name = "destination_wallet_id", nullable = false)
    private UUID destinationWalletId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Transfer() {
    }

    private Transfer(
            UUID id,
            UUID sourceWalletId,
            UUID destinationWalletId,
            BigDecimal amount,
            String currency,
            TransferStatus status,
            OffsetDateTime createdAt) {

        this.id = id;
        this.sourceWalletId = sourceWalletId;
        this.destinationWalletId = destinationWalletId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Transfer create(
            UUID sourceWalletId,
            UUID destinationWalletId,
            BigDecimal amount,
            String currency) {

        return new Transfer(
                UUID.randomUUID(),
                sourceWalletId,
                destinationWalletId,
                amount,
                currency,
                TransferStatus.COMPLETED,
                OffsetDateTime.now(ZoneOffset.UTC));
    }

    public static Transfer createAt(
            UUID sourceWalletId,
            UUID destinationWalletId,
            BigDecimal amount,
            String currency,
            OffsetDateTime createdAt) {

        return new Transfer(
                UUID.randomUUID(),
                sourceWalletId,
                destinationWalletId,
                amount,
                currency,
                TransferStatus.COMPLETED,
                createdAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceWalletId() {
        return sourceWalletId;
    }

    public UUID getDestinationWalletId() {
        return destinationWalletId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
