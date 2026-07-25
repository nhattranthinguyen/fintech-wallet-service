package com.nhattranthinguyen.wallet.ledger.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nhattranthinguyen.wallet.ledger.domain.LedgerEntry;
import com.nhattranthinguyen.wallet.ledger.infrastructure.LedgerEntryRepository;

@Service
public class LedgerService {
    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerService(LedgerEntryRepository ledgerEntryRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> getWalletHistory(UUID walletId) {
        return ledgerEntryRepository.findByWalletIdOrderByCreatedAtDesc(walletId);
    }
}
