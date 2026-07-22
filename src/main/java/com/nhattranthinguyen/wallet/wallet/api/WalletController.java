package com.nhattranthinguyen.wallet.wallet.api;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.nhattranthinguyen.wallet.wallet.application.WalletService;
import com.nhattranthinguyen.wallet.wallet.domain.Wallet;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {
    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(
        @Valid @RequestBody CreateWalletRequest request
    ) {
        Wallet wallet = walletService.createWallet(
            request.ownerId(),
            request.currency()
        );

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{walletId}")
            .buildAndExpand(wallet.getId())
            .toUri();

        return ResponseEntity
            .created(location)
            .body(WalletResponse.from(wallet));
    }

    @GetMapping("/{walletId}")
    public WalletResponse getWallet(
        @PathVariable UUID walletId
    ) {
        Wallet wallet = walletService.getWallet(walletId);
        return WalletResponse.from(wallet);
    }
}
