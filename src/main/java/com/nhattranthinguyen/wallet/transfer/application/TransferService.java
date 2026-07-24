package com.nhattranthinguyen.wallet.transfer.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nhattranthinguyen.wallet.ledger.domain.LedgerEntry;
import com.nhattranthinguyen.wallet.ledger.domain.LedgerEntryType;
import com.nhattranthinguyen.wallet.ledger.infrastructure.LedgerEntryRepository;
import com.nhattranthinguyen.wallet.transfer.application.exception.CurrencyMismatchException;
import com.nhattranthinguyen.wallet.transfer.application.exception.SameWalletTransferException;
import com.nhattranthinguyen.wallet.transfer.domain.Transfer;
import com.nhattranthinguyen.wallet.transfer.infrastructure.TransferRepository;
import com.nhattranthinguyen.wallet.wallet.application.exception.InvalidTransferAmountException;
import com.nhattranthinguyen.wallet.wallet.application.exception.WalletNotFoundException;
import com.nhattranthinguyen.wallet.wallet.domain.Wallet;
import com.nhattranthinguyen.wallet.wallet.infrastructure.WalletRepository;

@Service
public class TransferService {
    private final WalletRepository walletRepository;
    private final TransferRepository transferRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public TransferService(
            WalletRepository walletRepository,
            TransferRepository transferRepository,
            LedgerEntryRepository ledgerEntryRepository) {

        this.walletRepository = walletRepository;
        this.transferRepository = transferRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public Transfer execute(TransferCommand command) {
        validateCommand(command);

        Wallet sourceWallet = findWallet(command.sourceWalletId());

        Wallet destinationWallet = findWallet(command.destinationWalletId());

        validateCurrencies(sourceWallet, destinationWallet);

        sourceWallet.debit(command.amount());
        destinationWallet.credit(command.amount());

        Transfer transfer = Transfer.create(
                sourceWallet.getId(),
                destinationWallet.getId(),
                command.amount(),
                sourceWallet.getCurrency());

        transferRepository.save(transfer);

        LedgerEntry debitEntry = LedgerEntry.create(
                sourceWallet.getId(),
                transfer.getId(),
                LedgerEntryType.DEBIT,
                command.amount(),
                sourceWallet.getBalance());

        LedgerEntry creditEntry = LedgerEntry.create(
                destinationWallet.getId(),
                transfer.getId(),
                LedgerEntryType.CREDIT,
                command.amount(),
                destinationWallet.getBalance());

        ledgerEntryRepository.saveAll(
                List.of(debitEntry, creditEntry));

        return transfer;
    }

    private void validateCommand(TransferCommand command) {
        if (command == null) {
            throw new IllegalArgumentException(
                    "Transfer command must not be null.");
        }

        validateWalletIds(
                command.sourceWalletId(),
                command.destinationWalletId());

        validateAmount(command.amount());
    }

    private void validateWalletIds(
            UUID sourceWalletId,
            UUID destinationWalletId) {

        if (sourceWalletId == null
                || destinationWalletId == null) {

            throw new IllegalArgumentException(
                    "Source and destination wallet IDs are required.");
        }

        if (sourceWalletId.equals(destinationWalletId)) {
            throw new SameWalletTransferException();
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidTransferAmountException();
        }
    }

    private void validateCurrencies(
            Wallet sourceWallet,
            Wallet destinationWallet) {

        if (!sourceWallet.getCurrency()
                .equals(destinationWallet.getCurrency())) {

            throw new CurrencyMismatchException();
        }
    }

    private Wallet findWallet(UUID walletId) {
        return walletRepository
                .findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
    }
}
