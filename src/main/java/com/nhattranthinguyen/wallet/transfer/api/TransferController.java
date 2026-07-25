package com.nhattranthinguyen.wallet.transfer.api;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.nhattranthinguyen.wallet.transfer.application.TransferCommand;
import com.nhattranthinguyen.wallet.transfer.application.TransferService;
import com.nhattranthinguyen.wallet.transfer.domain.Transfer;
import com.nhattranthinguyen.wallet.shared.api.ApiErrorResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/transfers")
@Tag(name = "Transfers", description = "Transfer money between wallets")
public class TransferController {
    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    @Operation(
            summary = "Create a transfer",
            description = "Atomically debits the source wallet, credits the destination wallet, and records ledger entries.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(
                            name = "USD transfer",
                            value = """
                                    {
                                      "sourceWalletId": "11111111-1111-1111-1111-111111111111",
                                      "destinationWalletId": "33333333-3333-3333-3333-333333333333",
                                      "amount": 25.0000
                                    }
                                    """))))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transfer completed", headers = @Header(name = "Location", description = "URI of the created transfer"), content = @Content(schema = @Schema(implementation = TransferResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request validation failed", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Source or destination wallet not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Insufficient balance, currency mismatch, or identical wallets", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<TransferResponse> createTransfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        TransferCommand command = new TransferCommand(
                request.sourceWalletId(),
                request.destinationWalletId(),
                request.amount());

        Transfer transfer = transferService.execute(command, idempotencyKey);

        TransferResponse response = TransferResponse.from(transfer);

        URI location = URI.create(
                "/api/v1/transfers/" + transfer.getId());

        return ResponseEntity
                .created(location)
                .body(response);
    }
}
