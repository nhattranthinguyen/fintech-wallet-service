package com.nhattranthinguyen.wallet.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.nhattranthinguyen.wallet.wallet.application.exception.WalletAlreadyExistsException;
import com.nhattranthinguyen.wallet.wallet.application.exception.WalletNotFoundException;
import com.nhattranthinguyen.wallet.wallet.domain.Wallet;
import com.nhattranthinguyen.wallet.wallet.infrastructure.WalletRepository;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {
    @Mock
    private WalletRepository walletRepository;

    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletService = new WalletService(walletRepository);
    }

    @Test
    void shouldCreateWallet() {
        UUID ownerId = UUID.randomUUID();

        when(walletRepository.existsByOwnerIdAndCurrency(
                ownerId,
                "USD")).thenReturn(false);

        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Wallet result = walletService.createWallet(ownerId, "usd");

        assertThat(result.getId()).isNotNull();
        assertThat(result.getOwnerId()).isEqualTo(ownerId);
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.getVersion()).isNull();

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);

        verify(walletRepository).save(walletCaptor.capture());

        Wallet savedWallet = walletCaptor.getValue();

        assertThat(savedWallet.getOwnerId()).isEqualTo(ownerId);
        assertThat(savedWallet.getCurrency()).isEqualTo("USD");
    }

    @Test
    void shouldThrowWhenWalletAlreadyExistsDuringPreCheck() {
        UUID ownerId = UUID.randomUUID();

        when(walletRepository.existsByOwnerIdAndCurrency(
                ownerId,
                "USD")).thenReturn(true);

        assertThatThrownBy(() -> walletService.createWallet(ownerId, "usd"))
                .isInstanceOf(WalletAlreadyExistsException.class);

        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void shouldConvertDatabaseConstraintViolationToWalletAlreadyExists() {
        UUID ownerId = UUID.randomUUID();

        when(walletRepository.existsByOwnerIdAndCurrency(
                ownerId,
                "USD")).thenReturn(false);

        when(walletRepository.save(any(Wallet.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Unique constraint violation"));

        assertThatThrownBy(() -> walletService.createWallet(ownerId, "USD"))
                .isInstanceOf(WalletAlreadyExistsException.class);
    }

    @Test
    void shouldReturnWalletById() {
        Wallet wallet = Wallet.create(
                UUID.randomUUID(),
                "USD");

        when(walletRepository.findById(wallet.getId()))
                .thenReturn(Optional.of(wallet));

        Wallet result = walletService.getWallet(wallet.getId());

        assertThat(result).isSameAs(wallet);

        verify(walletRepository).findById(wallet.getId());
    }

    @Test
    void shouldThrowWhenWalletCannotBeFound() {
        UUID walletId = UUID.randomUUID();

        when(walletRepository.findById(walletId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getWallet(walletId))
                .isInstanceOf(WalletNotFoundException.class);
    }
}
