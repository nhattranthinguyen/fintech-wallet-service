package com.nhattranthinguyen.wallet.wallet.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.nhattranthinguyen.wallet.wallet.domain.Wallet;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    boolean existsByOwnerIdAndCurrency(UUID ownerId, String currency);

    Optional<Wallet> findByOwnerIdAndCurrency(UUID ownerId, String currency);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select wallet from Wallet wallet where wallet.id = :walletId")
    Optional<Wallet> findByIdForUpdate(@Param("walletId") UUID walletId);
}
