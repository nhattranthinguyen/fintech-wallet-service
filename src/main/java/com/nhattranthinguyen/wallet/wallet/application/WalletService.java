package com.nhattranthinguyen.wallet.wallet.application;

import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nhattranthinguyen.wallet.wallet.application.exception.WalletAlreadyExistsException;
import com.nhattranthinguyen.wallet.wallet.application.exception.WalletNotFoundException;
import com.nhattranthinguyen.wallet.wallet.domain.Wallet;
import com.nhattranthinguyen.wallet.wallet.infrastructure.WalletRepository;

@Service
public class WalletService {
    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Transactional
    public Wallet createWallet(UUID ownerId, String currency) {
        String normalizedCurrency = normalizeCurrency(currency);

        if (walletRepository.existsByOwnerIdAndCurrency(
                ownerId,
                normalizedCurrency)) {
            throw new WalletAlreadyExistsException(
                    ownerId,
                    normalizedCurrency);
        }

        Wallet wallet = Wallet.create(ownerId, normalizedCurrency);

        try {
            return walletRepository.save(wallet);
        } catch (DataIntegrityViolationException exception) {
            throw new WalletAlreadyExistsException(
                    ownerId,
                    normalizedCurrency);
        }
    }

    @Transactional(readOnly = true)
    public Wallet getWallet(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
    }

    private String normalizeCurrency(String currency) {
        return currency.trim().toUpperCase(Locale.ROOT);
    }
}
