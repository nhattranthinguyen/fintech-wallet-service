package com.nhattranthinguyen.wallet.transfer.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.nhattranthinguyen.wallet.ledger.infrastructure.LedgerEntryRepository;
import com.nhattranthinguyen.wallet.support.PostgresIT;
import com.nhattranthinguyen.wallet.transfer.application.TransferCommand;
import com.nhattranthinguyen.wallet.transfer.application.TransferService;
import com.nhattranthinguyen.wallet.transfer.infrastructure.IdempotencyKeyRepository;
import com.nhattranthinguyen.wallet.transfer.infrastructure.TransferRepository;
import com.nhattranthinguyen.wallet.wallet.domain.Wallet;
import com.nhattranthinguyen.wallet.wallet.infrastructure.WalletRepository;

@SpringBootTest
class TransferRollbackIT extends PostgresIT {
    @Autowired TransferService transferService;
    @Autowired WalletRepository walletRepository;
    @Autowired TransferRepository transferRepository;
    @Autowired LedgerEntryRepository ledgerEntryRepository;
    @Autowired IdempotencyKeyRepository idempotencyKeyRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        idempotencyKeyRepository.deleteAll();
        ledgerEntryRepository.deleteAll();
        transferRepository.deleteAll();
        walletRepository.deleteAll();
    }

    @Test
    void shouldRollbackEntireTransferWhenLedgerWriteFails() {
        Wallet source = Wallet.create(UUID.randomUUID(), "USD");
        source.credit(new BigDecimal("100.00"));
        Wallet destination = Wallet.create(UUID.randomUUID(), "USD");
        walletRepository.saveAll(List.of(source, destination));

        jdbcTemplate.execute("create function reject_ledger_write() returns trigger language plpgsql as $$ begin raise exception 'forced ledger failure'; end $$");
        jdbcTemplate.execute("create trigger reject_ledger before insert on ledger_entries for each row execute function reject_ledger_write() ");
        try {
            assertThatThrownBy(() -> transferService.execute(new TransferCommand(
                    source.getId(), destination.getId(), new BigDecimal("25.00"))))
                    .isInstanceOf(RuntimeException.class);
        } finally {
            jdbcTemplate.execute("drop trigger if exists reject_ledger on ledger_entries");
            jdbcTemplate.execute("drop function if exists reject_ledger_write()");
        }

        assertThat(walletRepository.findById(source.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("100.00");
        assertThat(walletRepository.findById(destination.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("0.00");
        assertThat(transferRepository.count()).isZero();
        assertThat(ledgerEntryRepository.count()).isZero();
    }
}
