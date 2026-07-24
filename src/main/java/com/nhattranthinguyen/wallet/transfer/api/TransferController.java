package com.nhattranthinguyen.wallet.transfer.api;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhattranthinguyen.wallet.transfer.application.TransferCommand;
import com.nhattranthinguyen.wallet.transfer.application.TransferService;
import com.nhattranthinguyen.wallet.transfer.domain.Transfer;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {
    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> createTransfer(
            @Valid @RequestBody TransferRequest request) {
        TransferCommand command = new TransferCommand(
                request.sourceWalletId(),
                request.destinationWalletId(),
                request.amount());

        Transfer transfer = transferService.execute(command);

        TransferResponse response = TransferResponse.from(transfer);

        URI location = URI.create(
                "/api/v1/transfers/" + transfer.getId());

        return ResponseEntity
                .created(location)
                .body(response);
    }
}
