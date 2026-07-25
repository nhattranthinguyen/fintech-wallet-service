package com.nhattranthinguyen.wallet.ledger.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
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

    @Test
    void shouldReturnOnlyEntriesForRequestedWallet() {
        Wallet requestedWallet = persistWallet();
        Wallet otherWallet = persistWallet();

        LedgerEntry requestedEntry = entryAt(
                requestedWallet.getId(), "10.0000", 1);
        LedgerEntry otherEntry = entryAt(
                otherWallet.getId(), "20.0000", 2);
        ledgerEntryRepository.saveAllAndFlush(List.of(requestedEntry, otherEntry));

        entityManager.clear();

        List<LedgerEntry> result = ledgerEntryRepository
                .findByWalletIdOrderByCreatedAtDesc(requestedWallet.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId())
                .isEqualTo(requestedEntry.getId());
    }

    @Test
    void shouldReturnWalletEntriesNewestFirst() {
        Wallet wallet = persistWallet();
        LedgerEntry oldest = entryAt(wallet.getId(), "10.0000", 1);
        LedgerEntry newest = entryAt(wallet.getId(), "30.0000", 3);
        LedgerEntry middle = entryAt(wallet.getId(), "20.0000", 2);
        ledgerEntryRepository.saveAllAndFlush(List.of(oldest, newest, middle));

        entityManager.clear();

        List<LedgerEntry> result = ledgerEntryRepository
                .findByWalletIdOrderByCreatedAtDesc(wallet.getId());

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getId()).isEqualTo(newest.getId());
        assertThat(result.get(1).getId()).isEqualTo(middle.getId());
        assertThat(result.get(2).getId()).isEqualTo(oldest.getId());
    }

    private LedgerEntry entryAt(UUID walletId, String amount, int hour) {
        BigDecimal value = new BigDecimal(amount);
        return LedgerEntry.createAt(
                walletId,
                null,
                LedgerEntryType.CREDIT,
                value,
                value,
                OffsetDateTime.of(2026, 7, 25, hour, 0, 0, 0, ZoneOffset.UTC));
    }

    private Wallet persistWallet() {
        Wallet wallet = Wallet.create(
                UUID.randomUUID(),
                CURRENCY);

        return walletRepository.saveAndFlush(wallet);
    }
}
