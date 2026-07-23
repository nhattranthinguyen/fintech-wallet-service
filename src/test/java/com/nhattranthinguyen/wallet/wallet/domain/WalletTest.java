package com.nhattranthinguyen.wallet.wallet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

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
}
