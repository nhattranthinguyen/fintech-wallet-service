package com.nhattranthinguyen.wallet.transfer.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.nhattranthinguyen.wallet.support.PostgresIT;
import com.nhattranthinguyen.wallet.transfer.domain.Transfer;
import com.nhattranthinguyen.wallet.transfer.domain.TransferStatus;
import com.nhattranthinguyen.wallet.transfer.infrastructure.TransferRepository;
import com.nhattranthinguyen.wallet.wallet.domain.Wallet;
import com.nhattranthinguyen.wallet.wallet.infrastructure.WalletRepository;

import jakarta.persistence.EntityManager;

@DataJpaTest
class TransferRepositoryIT extends PostgresIT {
    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistTransfer() {
        Wallet sourceWallet = persistWallet();
        Wallet destinationWallet = persistWallet();

        Transfer transfer = Transfer.create(
                sourceWallet.getId(),
                destinationWallet.getId(),
                new BigDecimal("100.0000"),
                "USD");

        transferRepository.saveAndFlush(transfer);

        entityManager.clear();

        Transfer persisted = transferRepository.findById(transfer.getId())
                .orElseThrow();

        assertThat(persisted.getId()).isEqualTo(transfer.getId());
        assertThat(persisted.getSourceWalletId()).isEqualTo(sourceWallet.getId());
        assertThat(persisted.getDestinationWalletId()).isEqualTo(destinationWallet.getId());
        assertThat(persisted.getAmount()).isEqualByComparingTo("100.0000");
        assertThat(persisted.getCurrency()).isEqualTo("USD");
        assertThat(persisted.getStatus()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(persisted.getCreatedAt()).isNotNull();
    }

    private Wallet persistWallet() {
        return walletRepository.saveAndFlush(
                Wallet.create(UUID.randomUUID(), "USD"));
    }
}
