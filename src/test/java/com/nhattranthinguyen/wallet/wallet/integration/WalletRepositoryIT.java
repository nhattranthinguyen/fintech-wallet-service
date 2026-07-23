package com.nhattranthinguyen.wallet.wallet.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.nhattranthinguyen.wallet.support.PostgresIT;
import com.nhattranthinguyen.wallet.wallet.domain.Wallet;
import com.nhattranthinguyen.wallet.wallet.infrastructure.WalletRepository;

import jakarta.persistence.EntityManager;

@DataJpaTest
class WalletRepositoryIT extends PostgresIT {
    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistWalletAndInitializeVersion() {
        Wallet wallet = Wallet.create(
                UUID.randomUUID(),
                "USD");

        assertThat(wallet.getVersion()).isNull();

        Wallet savedWallet = walletRepository.saveAndFlush(wallet);

        assertThat(savedWallet.getId()).isNotNull();
        assertThat(savedWallet.getVersion()).isNotNull();
        assertThat(savedWallet.getVersion()).isZero();
    }

    @Test
    void shouldFindWalletById() {
        Wallet savedWallet = walletRepository.saveAndFlush(
                Wallet.create(UUID.randomUUID(), "USD"));

        entityManager.clear();

        assertThat(walletRepository.findById(savedWallet.getId()))
                .isPresent()
                .get()
                .satisfies(wallet -> {
                    assertThat(wallet.getId())
                            .isEqualTo(savedWallet.getId());

                    assertThat(wallet.getCurrency())
                            .isEqualTo("USD");
                });
    }

    @Test
    void shouldCheckExistenceByOwnerAndCurrency() {
        UUID ownerId = UUID.randomUUID();

        walletRepository.saveAndFlush(
                Wallet.create(ownerId, "USD"));

        assertThat(
                walletRepository.existsByOwnerIdAndCurrency(
                        ownerId,
                        "USD"))
                .isTrue();

        assertThat(
                walletRepository.existsByOwnerIdAndCurrency(
                        ownerId,
                        "EUR"))
                .isFalse();
    }

    @Test
    void shouldRejectDuplicateOwnerAndCurrency() {
        UUID ownerId = UUID.randomUUID();

        walletRepository.saveAndFlush(
                Wallet.create(ownerId, "USD"));

        Wallet duplicate = Wallet.create(ownerId, "USD");

        assertThatThrownBy(() -> walletRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
