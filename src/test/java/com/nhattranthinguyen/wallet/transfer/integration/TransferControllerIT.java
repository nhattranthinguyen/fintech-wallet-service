package com.nhattranthinguyen.wallet.transfer.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.nhattranthinguyen.wallet.ledger.infrastructure.LedgerEntryRepository;
import com.nhattranthinguyen.wallet.support.PostgresIT;
import com.nhattranthinguyen.wallet.transfer.api.TransferRequest;
import com.nhattranthinguyen.wallet.transfer.domain.Transfer;
import com.nhattranthinguyen.wallet.transfer.infrastructure.TransferRepository;
import com.nhattranthinguyen.wallet.transfer.infrastructure.IdempotencyKeyRepository;
import com.nhattranthinguyen.wallet.wallet.domain.Wallet;
import com.nhattranthinguyen.wallet.wallet.infrastructure.WalletRepository;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
public class TransferControllerIT extends PostgresIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @BeforeEach
    void cleanDatabase() {
        idempotencyKeyRepository.deleteAll();
        ledgerEntryRepository.deleteAll();
        transferRepository.deleteAll();
        walletRepository.deleteAll();
    }

    @Test
    void shouldCreateTransferSuccessfully() throws Exception {
        Wallet source = Wallet.create(UUID.randomUUID(), "USD");
        source.credit(new BigDecimal("100.00"));

        Wallet destination = Wallet.create(UUID.randomUUID(), "USD");

        walletRepository.save(source);
        walletRepository.save(destination);

        TransferRequest request = new TransferRequest(
                source.getId(),
                destination.getId(),
                new BigDecimal("25.00"));

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.sourceWalletId")
                        .value(source.getId().toString()))
                .andExpect(jsonPath("$.destinationWalletId")
                        .value(destination.getId().toString()))
                .andExpect(jsonPath("$.amount")
                        .value(25.00))
                .andExpect(jsonPath("$.currency")
                        .value("USD"))
                .andExpect(jsonPath("$.status")
                        .value("COMPLETED"));

        List<Transfer> transfers = transferRepository.findAll();

        assertThat(transfers).hasSize(1);

        Transfer transfer = transfers.get(0);

        assertThat(transfer.getSourceWalletId())
                .isEqualTo(source.getId());

        assertThat(transfer.getDestinationWalletId())
                .isEqualTo(destination.getId());

        assertThat(transfer.getAmount())
                .isEqualByComparingTo("25.00");

        assertThat(ledgerEntryRepository.findAll())
                .hasSize(2);

        Wallet updatedSource = walletRepository.findById(source.getId())
                .orElseThrow();

        Wallet updatedDestination = walletRepository.findById(destination.getId())
                .orElseThrow();

        assertThat(updatedSource.getBalance())
                .isEqualByComparingTo("75.00");

        assertThat(updatedDestination.getBalance())
                .isEqualByComparingTo("25.00");

        mockMvc.perform(get("/api/v1/wallets/{walletId}/transactions", source.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transferId").value(transfer.getId().toString()))
                .andExpect(jsonPath("$[0].type").value("DEBIT"))
                .andExpect(jsonPath("$[0].amount").value(25.00))
                .andExpect(jsonPath("$[0].balanceAfter").value(75.00));
    }

    @Test
    void shouldReturnExistingTransferForRepeatedIdempotencyKey() throws Exception {
        Wallet source = Wallet.create(UUID.randomUUID(), "USD");
        source.credit(new BigDecimal("100.00"));
        Wallet destination = Wallet.create(UUID.randomUUID(), "USD");
        walletRepository.saveAll(List.of(source, destination));

        TransferRequest request = new TransferRequest(
                source.getId(), destination.getId(), new BigDecimal("25.00"));
        String body = objectMapper.writeValueAsString(request);

        String firstResponse = mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", "transfer-123")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String secondResponse = mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", "transfer-123")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(secondResponse).get("id").asText())
                .isEqualTo(objectMapper.readTree(firstResponse).get("id").asText());
        assertThat(transferRepository.count()).isEqualTo(1);
        assertThat(ledgerEntryRepository.count()).isEqualTo(2);
        assertThat(walletRepository.findById(source.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("75.00");
    }

    @Test
    void shouldReturnBadRequestWhenAmountIsZero() throws Exception {

        TransferRequest request = new TransferRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.ZERO);

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenRequestIsInvalid() throws Exception {

        String json = """
            {
              "sourceWalletId": null,
              "destinationWalletId": null,
              "amount": null
            }
            """;

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFoundWhenSourceWalletDoesNotExist() throws Exception {

        Wallet destination = Wallet.create(UUID.randomUUID(), "USD");
        walletRepository.save(destination);

        TransferRequest request = new TransferRequest(
                UUID.randomUUID(),
                destination.getId(),
                new BigDecimal("10"));

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnConflictWhenInsufficientBalance() throws Exception {

        Wallet source = Wallet.create(UUID.randomUUID(), "USD");
        source.credit(new BigDecimal("10"));

        Wallet destination = Wallet.create(UUID.randomUUID(), "USD");

        walletRepository.save(source);
        walletRepository.save(destination);

        TransferRequest request = new TransferRequest(
                source.getId(),
                destination.getId(),
                new BigDecimal("50"));

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturnConflictWhenCurrenciesDoNotMatch() throws Exception {

        Wallet source = Wallet.create(UUID.randomUUID(), "USD");
        source.credit(new BigDecimal("100"));

        Wallet destination = Wallet.create(UUID.randomUUID(), "EUR");

        walletRepository.save(source);
        walletRepository.save(destination);

        TransferRequest request = new TransferRequest(
                source.getId(),
                destination.getId(),
                new BigDecimal("10"));

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
