package com.nhattranthinguyen.wallet.wallet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.nhattranthinguyen.wallet.wallet.application.exception.InsufficientBalanceException;

class WalletTest {
    @Test
    void shouldCreateWalletWithInitialValues() {
        UUID ownerId = UUID.randomUUID();

        Wallet wallet = Wallet.create(ownerId, "USD");

        assertThat(wallet.getId()).isNotNull();
        assertThat(wallet.getOwnerId()).isEqualTo(ownerId);
        assertThat(wallet.getCurrency()).isEqualTo("USD");
        assertThat(wallet.getBalance())
            .isEqualByComparingTo(new BigDecimal("0.0000"));

        assertThat(wallet.getVersion()).isNull();

        assertThat(wallet.getCreatedAt()).isNotNull();
        assertThat(wallet.getUpdatedAt()).isNotNull();
        assertThat(wallet.getUpdatedAt())
            .isEqualTo(wallet.getCreatedAt());
    }

    @Test
    void shouldNormalizeCurrency() {
        Wallet wallet = Wallet.create(
            UUID.randomUUID(),
            " usd "
        );

        assertThat(wallet.getCurrency()).isEqualTo("USD");
    }

    @Test
    void shouldRejectNullOwnerId() {
        assertThatThrownBy(() -> Wallet.create(null, "USD"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Owner");
    }

    @Test
    void shouldRejectNullCurrency() {
        assertThatThrownBy(() ->
            Wallet.create(UUID.randomUUID(), null)
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Currency");
    }

    @Test
    void shouldRejectBlankCurrency() {
        assertThatThrownBy(() ->
            Wallet.create(UUID.randomUUID(), "   ")
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Currency");
    }

    @Test
    void shouldRejectCurrencyThatIsNotThreeCharacters() {
        assertThatThrownBy(() ->
            Wallet.create(UUID.randomUUID(), "US")
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("3");
    }

    @Test
    void shouldCreditWallet() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "USD");

        wallet.credit(new BigDecimal("25.0000"));

        assertThat(wallet.getBalance())
                .isEqualByComparingTo("25.0000");
    }

    @Test
    void shouldThrowWhenCreditingZero() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "USD");

        assertThatThrownBy(() ->
                wallet.credit(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowWhenCreditingNegativeAmount() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "USD");

        assertThatThrownBy(() ->
                wallet.credit(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldDebitWallet() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "USD");

        wallet.credit(new BigDecimal("100.0000"));

        wallet.debit(new BigDecimal("35.0000"));

        assertThat(wallet.getBalance())
                .isEqualByComparingTo("65.0000");
    }

    @Test
    void shouldThrowWhenDebitingZero() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "USD");

        assertThatThrownBy(() ->
                wallet.debit(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowWhenDebitingNegativeAmount() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "USD");

        assertThatThrownBy(() ->
                wallet.debit(new BigDecimal("-5")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowWhenDebitingMoreThanBalance() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "USD");

        wallet.credit(new BigDecimal("10.0000"));

        assertThatThrownBy(() ->
                wallet.debit(new BigDecimal("20.0000")))
                .isInstanceOf(InsufficientBalanceException.class);
    }
}
