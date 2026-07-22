package com.nhattranthinguyen.wallet.wallet.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nhattranthinguyen.wallet.wallet.domain.Wallet;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    boolean existsByOwnerIdAndCurrency(UUID ownerId, String currency);

    Optional<Wallet> findByOwnerIdAndCurrency(UUID ownerId, String currency);

}
