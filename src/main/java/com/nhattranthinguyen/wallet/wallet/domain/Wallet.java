package com.nhattranthinguyen.wallet.wallet.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
    name = "wallets",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_wallet_owner_currency",
            columnNames = {"owner_id", "currency"}
        )
    }
)
public class Wallet {
    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Wallet() {}

    private Wallet(UUID id, UUID ownerId, String currency) {
        this.id = id;
        this.ownerId = ownerId;
        this.currency = currency;
        this.balance = BigDecimal.ZERO.setScale(4);
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.updatedAt = this.createdAt;
    }

    public static Wallet create(UUID ownerId, String currency) {
        if (ownerId == null) {
            throw new IllegalArgumentException("Owner ID is required");
        }

        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }

        String normalizedCurrency = currency.trim().toUpperCase();

        if (normalizedCurrency.length() != 3) {
            throw new IllegalArgumentException(
                "Currency must contain exactly 3 characters"
            );
        }

        return new Wallet(
            UUID.randomUUID(),
            ownerId,
            normalizedCurrency
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Long getVersion() {
        return version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
