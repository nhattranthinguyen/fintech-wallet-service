package com.nhattranthinguyen.wallet.ledger.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.nhattranthinguyen.wallet.ledger.domain.LedgerEntry;
import com.nhattranthinguyen.wallet.ledger.domain.LedgerEntryType;
import com.nhattranthinguyen.wallet.ledger.infrastructure.LedgerEntryRepository;
import com.nhattranthinguyen.wallet.support.PostgresIT;
import com.nhattranthinguyen.wallet.wallet.domain.Wallet;
import com.nhattranthinguyen.wallet.wallet.infrastructure.WalletRepository;

import jakarta.persistence.EntityManager;

@DataJpaTest
class LedgerEntryRepositoryIT extends PostgresIT {
    private static final String CURRENCY = "USD";

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistLedgerEntry() {
        Wallet wallet = persistWallet();

        LedgerEntry entry = LedgerEntry.create(
                wallet.getId(),
                null,
                LedgerEntryType.CREDIT,
                new BigDecimal("25.0000"),
                new BigDecimal("25.0000"));

        LedgerEntry saved = ledgerEntryRepository.saveAndFlush(entry);

        entityManager.clear();

        LedgerEntry found = ledgerEntryRepository
                .findById(saved.getId())
                .orElseThrow();

        assertThat(found.getWalletId())
                .isEqualTo(wallet.getId());

        assertThat(found.getTransferId())
                .isNull();

        assertThat(found.getEntryType())
                .isEqualTo(LedgerEntryType.CREDIT);

        assertThat(found.getAmount())
                .isEqualByComparingTo("25.0000");

        assertThat(found.getBalanceAfter())
                .isEqualByComparingTo("25.0000");

        assertThat(found.getCreatedAt())
                .isNotNull();
    }

    private Wallet persistWallet() {
        Wallet wallet = Wallet.create(
                UUID.randomUUID(),
                CURRENCY);

        return walletRepository.saveAndFlush(wallet);
    }
}
