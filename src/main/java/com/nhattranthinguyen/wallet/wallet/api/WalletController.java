package com.nhattranthinguyen.wallet.wallet.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.nhattranthinguyen.wallet.ledger.api.LedgerEntryResponse;
import com.nhattranthinguyen.wallet.ledger.application.LedgerService;
import com.nhattranthinguyen.wallet.shared.api.ApiErrorResponse;
import com.nhattranthinguyen.wallet.wallet.application.WalletService;
import com.nhattranthinguyen.wallet.wallet.domain.Wallet;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/wallets")
@Tag(name = "Wallets", description = "Create wallets and retrieve their balances")
public class WalletController {
    private final WalletService walletService;
    private final LedgerService ledgerService;

    public WalletController(WalletService walletService, LedgerService ledgerService) {
        this.walletService = walletService;
        this.ledgerService = ledgerService;
    }

    @PostMapping
    @Operation(
        summary = "Create a wallet",
        description = "Creates one wallet per owner and ISO-style three-letter currency.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(
                name = "USD wallet",
                value = """
                    {
                      "ownerId": "22222222-2222-2222-2222-222222222222",
                      "currency": "USD"
                    }
                    """
            ))
        )
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Wallet created",
            headers = @Header(
                name = "Location",
                description = "URI of the created wallet"
            ),
            content = @Content(schema = @Schema(implementation = WalletResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Request validation failed",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "A wallet already exists for this owner and currency",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
        )
    })
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
    @Operation(summary = "Get a wallet", description = "Returns a wallet and its current balance.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Wallet found",
            content = @Content(schema = @Schema(implementation = WalletResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Wallet not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
        )
    })
    public WalletResponse getWallet(
        @Parameter(description = "Wallet identifier", example = "11111111-1111-1111-1111-111111111111")
        @PathVariable UUID walletId
    ) {
        Wallet wallet = walletService.getWallet(walletId);
        return WalletResponse.from(wallet);
    }

    @GetMapping("/{walletId}/transactions")
    @Operation(
        summary = "Get wallet transaction history",
        description = "Returns the wallet's ledger entries in reverse chronological order."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Transaction history returned",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = LedgerEntryResponse.class)))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Wallet not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
        )
    })
    public List<LedgerEntryResponse> getWalletTransactions(
        @Parameter(description = "Wallet identifier", example = "11111111-1111-1111-1111-111111111111")
        @PathVariable UUID walletId
    ) {
        walletService.getWallet(walletId);
        return ledgerService.getWalletHistory(walletId).stream()
            .map(LedgerEntryResponse::from)
            .toList();
    }
}
