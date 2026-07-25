package com.nhattranthinguyen.wallet.transfer.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.nhattranthinguyen.wallet.ledger.infrastructure.LedgerEntryRepository;
import com.nhattranthinguyen.wallet.support.PostgresIT;
import com.nhattranthinguyen.wallet.transfer.application.TransferCommand;
import com.nhattranthinguyen.wallet.transfer.application.TransferService;
import com.nhattranthinguyen.wallet.transfer.infrastructure.IdempotencyKeyRepository;
import com.nhattranthinguyen.wallet.transfer.infrastructure.TransferRepository;
import com.nhattranthinguyen.wallet.wallet.domain.Wallet;
import com.nhattranthinguyen.wallet.wallet.infrastructure.WalletRepository;

@SpringBootTest
class TransferConcurrencyIT extends PostgresIT {
    @Autowired TransferService transferService;
    @Autowired WalletRepository walletRepository;
    @Autowired TransferRepository transferRepository;
    @Autowired LedgerEntryRepository ledgerEntryRepository;
    @Autowired IdempotencyKeyRepository idempotencyKeyRepository;

    @BeforeEach
    void cleanDatabase() {
        idempotencyKeyRepository.deleteAll();
        ledgerEntryRepository.deleteAll();
        transferRepository.deleteAll();
        walletRepository.deleteAll();
    }

    @Test
    void shouldPreventConcurrentTransfersFromOverspending() throws Exception {
        Wallet source = Wallet.create(UUID.randomUUID(), "USD");
        source.credit(new BigDecimal("100.00"));
        Wallet firstDestination = Wallet.create(UUID.randomUUID(), "USD");
        Wallet secondDestination = Wallet.create(UUID.randomUUID(), "USD");
        walletRepository.saveAll(List.of(source, firstDestination, secondDestination));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> executeWhenReleased(
                    ready, start, new TransferCommand(source.getId(), firstDestination.getId(), new BigDecimal("75.00"))));
            Future<Boolean> second = executor.submit(() -> executeWhenReleased(
                    ready, start, new TransferCommand(source.getId(), secondDestination.getId(), new BigDecimal("75.00"))));
            ready.await();
            start.countDown();

            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);
        } finally {
            executor.shutdownNow();
        }

        assertThat(walletRepository.findById(source.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("25.00");
        assertThat(transferRepository.count()).isEqualTo(1);
        assertThat(ledgerEntryRepository.count()).isEqualTo(2);
    }

    private boolean executeWhenReleased(CountDownLatch ready, CountDownLatch start, TransferCommand command)
            throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            transferService.execute(command);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
