package com.nhattranthinguyen.wallet.transfer.application;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
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
import com.nhattranthinguyen.wallet.transfer.application.exception.IdempotencyConflictException;
import com.nhattranthinguyen.wallet.transfer.infrastructure.IdempotencyKey;
import com.nhattranthinguyen.wallet.transfer.infrastructure.IdempotencyKeyRepository;
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
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    public TransferService(
            WalletRepository walletRepository,
            TransferRepository transferRepository,
            LedgerEntryRepository ledgerEntryRepository,
            IdempotencyKeyRepository idempotencyKeyRepository) {

        this.walletRepository = walletRepository;
        this.transferRepository = transferRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    @Transactional
    public Transfer execute(TransferCommand command) {
        validateCommand(command);

        return executeTransfer(command);
    }

    @Transactional
    public Transfer execute(TransferCommand command, String idempotencyKey) {
        validateCommand(command);

        if (idempotencyKey == null) {
            return executeTransfer(command);
        }

        String normalizedKey = idempotencyKey.trim();
        if (normalizedKey.isEmpty() || normalizedKey.length() > 255) {
            throw new IllegalArgumentException("Idempotency-Key must contain between 1 and 255 characters.");
        }

        String requestHash = hash(command);
        idempotencyKeyRepository.lock("CREATE_TRANSFER:" + normalizedKey);

        var existingKey = idempotencyKeyRepository.findTransferKey(normalizedKey);
        if (existingKey.isPresent()) {
            IdempotencyKey storedKey = existingKey.get();
            if (!storedKey.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException();
            }
            return transferRepository.findById(storedKey.getResourceId())
                    .orElseThrow(() -> new IllegalStateException("Idempotent transfer result is missing."));
        }

        Transfer transfer = executeTransfer(command);
        idempotencyKeyRepository.save(IdempotencyKey.forTransfer(
                normalizedKey,
                requestHash,
                transfer.getId()));
        return transfer;
    }

    private Transfer executeTransfer(TransferCommand command) {
        UUID firstWalletId = command.sourceWalletId().compareTo(command.destinationWalletId()) < 0
                ? command.sourceWalletId()
                : command.destinationWalletId();
        UUID secondWalletId = firstWalletId.equals(command.sourceWalletId())
                ? command.destinationWalletId()
                : command.sourceWalletId();

        Wallet firstWallet = findWalletForUpdate(firstWalletId);
        Wallet secondWallet = findWalletForUpdate(secondWalletId);

        Wallet sourceWallet = firstWallet.getId().equals(command.sourceWalletId()) ? firstWallet : secondWallet;
        Wallet destinationWallet = firstWallet.getId().equals(command.destinationWalletId()) ? firstWallet : secondWallet;

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

    private String hash(TransferCommand command) {
        String payload = command.sourceWalletId()
                + ":" + command.destinationWalletId()
                + ":" + command.amount().stripTrailingZeros().toPlainString();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
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

    private Wallet findWalletForUpdate(UUID walletId) {
        return walletRepository
                .findByIdForUpdate(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
    }
}
