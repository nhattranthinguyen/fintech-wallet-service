package com.nhattranthinguyen.wallet.ledger.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nhattranthinguyen.wallet.ledger.domain.LedgerEntry;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
    List<LedgerEntry> findByWalletIdOrderByCreatedAtDesc(UUID walletId);
}
