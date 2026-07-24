package com.nhattranthinguyen.wallet.transfer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nhattranthinguyen.wallet.ledger.infrastructure.LedgerEntryRepository;
import com.nhattranthinguyen.wallet.transfer.application.exception.CurrencyMismatchException;
import com.nhattranthinguyen.wallet.transfer.application.exception.SameWalletTransferException;
import com.nhattranthinguyen.wallet.transfer.domain.Transfer;
import com.nhattranthinguyen.wallet.transfer.infrastructure.TransferRepository;
import com.nhattranthinguyen.wallet.wallet.application.exception.InsufficientBalanceException;
import com.nhattranthinguyen.wallet.wallet.application.exception.InvalidTransferAmountException;
import com.nhattranthinguyen.wallet.wallet.application.exception.WalletNotFoundException;
import com.nhattranthinguyen.wallet.wallet.domain.Wallet;
import com.nhattranthinguyen.wallet.wallet.infrastructure.WalletRepository;

@ExtendWith(MockitoExtension.class)
public class TransferServiceTest {
    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks
    private TransferService transferService;

    @Test
    void shouldTransferMoneySuccessfully() {
        Wallet source = Wallet.create(UUID.randomUUID(), "USD");
        Wallet destination = Wallet.create(UUID.randomUUID(), "USD");

        source.credit(new BigDecimal("100.00"));

        when(walletRepository.findById(source.getId()))
                .thenReturn(Optional.of(source));

        when(walletRepository.findById(destination.getId()))
                .thenReturn(Optional.of(destination));

        when(transferRepository.save(any(Transfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransferCommand command = new TransferCommand(
                source.getId(),
                destination.getId(),
                new BigDecimal("25.00"));

        Transfer transfer = transferService.execute(command);

        assertThat(transfer).isNotNull();

        assertThat(source.getBalance())
                .isEqualByComparingTo("75.00");

        assertThat(destination.getBalance())
                .isEqualByComparingTo("25.00");

        verify(transferRepository).save(any(Transfer.class));
        verify(ledgerEntryRepository).saveAll(any());
    }

    @Test
    void shouldThrowWhenAmountIsZero() {
        TransferCommand command = new TransferCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.ZERO);

        assertThatThrownBy(() -> transferService.execute(command))
                .isInstanceOf(InvalidTransferAmountException.class);

        verify(transferRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenAmountIsNegative() {
        TransferCommand command = new TransferCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("-10"));

        assertThatThrownBy(() -> transferService.execute(command))
                .isInstanceOf(InvalidTransferAmountException.class);

        verify(transferRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenSourceAndDestinationWalletAreSame() {
        UUID walletId = UUID.randomUUID();

        TransferCommand command = new TransferCommand(
                walletId,
                walletId,
                new BigDecimal("10"));

        assertThatThrownBy(() -> transferService.execute(command))
                .isInstanceOf(SameWalletTransferException.class);

        verify(transferRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenSourceWalletDoesNotExist() {
        UUID sourceId = UUID.randomUUID();
        UUID destinationId = UUID.randomUUID();

        when(walletRepository.findById(sourceId))
                .thenReturn(Optional.empty());

        TransferCommand command = new TransferCommand(
                sourceId,
                destinationId,
                new BigDecimal("10"));

        assertThatThrownBy(() -> transferService.execute(command))
                .isInstanceOf(WalletNotFoundException.class);

        verify(transferRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenDestinationWalletDoesNotExist() {
        Wallet source = Wallet.create(UUID.randomUUID(), "USD");

        when(walletRepository.findById(source.getId()))
                .thenReturn(Optional.of(source));

        UUID destinationId = UUID.randomUUID();

        when(walletRepository.findById(destinationId))
                .thenReturn(Optional.empty());

        TransferCommand command = new TransferCommand(
                source.getId(),
                destinationId,
                new BigDecimal("10"));

        assertThatThrownBy(() -> transferService.execute(command))
                .isInstanceOf(WalletNotFoundException.class);

        verify(transferRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenCurrenciesDoNotMatch() {
        Wallet source = Wallet.create(UUID.randomUUID(), "USD");
        Wallet destination = Wallet.create(UUID.randomUUID(), "EUR");

        source.credit(new BigDecimal("100"));

        when(walletRepository.findById(source.getId()))
                .thenReturn(Optional.of(source));

        when(walletRepository.findById(destination.getId()))
                .thenReturn(Optional.of(destination));

        TransferCommand command = new TransferCommand(
                source.getId(),
                destination.getId(),
                new BigDecimal("20"));

        assertThatThrownBy(() -> transferService.execute(command))
                .isInstanceOf(CurrencyMismatchException.class);

        verify(transferRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenSourceWalletHasInsufficientBalance() {
        Wallet source = Wallet.create(UUID.randomUUID(), "USD");
        Wallet destination = Wallet.create(UUID.randomUUID(), "USD");

        source.credit(new BigDecimal("10"));

        when(walletRepository.findById(source.getId()))
                .thenReturn(Optional.of(source));

        when(walletRepository.findById(destination.getId()))
                .thenReturn(Optional.of(destination));

        TransferCommand command = new TransferCommand(
                source.getId(),
                destination.getId(),
                new BigDecimal("20"));

        assertThatThrownBy(() -> transferService.execute(command))
                .isInstanceOf(InsufficientBalanceException.class);

        verify(transferRepository, never()).save(any());
    }
}
