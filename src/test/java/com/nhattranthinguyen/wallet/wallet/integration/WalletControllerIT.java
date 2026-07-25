package com.nhattranthinguyen.wallet.wallet.integration;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.nhattranthinguyen.wallet.support.PostgresIT;
import com.nhattranthinguyen.wallet.ledger.domain.LedgerEntry;
import com.nhattranthinguyen.wallet.ledger.domain.LedgerEntryType;
import com.nhattranthinguyen.wallet.ledger.infrastructure.LedgerEntryRepository;
import com.nhattranthinguyen.wallet.wallet.domain.Wallet;
import com.nhattranthinguyen.wallet.wallet.infrastructure.WalletRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class WalletControllerIT extends PostgresIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @BeforeEach
    void cleanDatabase() {
        ledgerEntryRepository.deleteAll();
        walletRepository.deleteAll();
    }

    @Test
    void shouldCreateAndRetrieveWallet() throws Exception {
        UUID ownerId = UUID.randomUUID();

        String response = mockMvc.perform(
                post("/api/v1/wallets")
                        .contentType("application/json")
                        .content("""
                                {
                                  "ownerId": "%s",
                                  "currency": "usd"
                                }
                                """.formatted(ownerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath(
                        "$.ownerId",
                        is(ownerId.toString())))
                .andExpect(jsonPath("$.currency", is("USD")))
                .andExpect(jsonPath("$.balance", is(0.0)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String walletId = extractJsonField(response, "id");

        mockMvc.perform(get(
                "/api/v1/wallets/{walletId}",
                walletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(walletId)))
                .andExpect(jsonPath(
                        "$.ownerId",
                        is(ownerId.toString())))
                .andExpect(jsonPath("$.currency", is("USD")));
    }

    @Test
    void shouldRejectDuplicateWallet() throws Exception {
        UUID ownerId = UUID.randomUUID();

        String requestBody = """
                {
                  "ownerId": "%s",
                  "currency": "USD"
                }
                """.formatted(ownerId);

        mockMvc.perform(post("/api/v1/wallets")
                .contentType("application/json")
                .content(requestBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/wallets")
                .contentType("application/json")
                .content(requestBody))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldAllowDifferentCurrenciesForSameOwner()
            throws Exception {

        UUID ownerId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/wallets")
                .contentType("application/json")
                .content("""
                        {
                          "ownerId": "%s",
                          "currency": "USD"
                        }
                        """.formatted(ownerId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/wallets")
                .contentType("application/json")
                .content("""
                        {
                          "ownerId": "%s",
                          "currency": "EUR"
                        }
                        """.formatted(ownerId)))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturnNotFoundForUnknownWallet()
            throws Exception {

        mockMvc.perform(get(
                "/api/v1/wallets/{walletId}",
                UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/wallets")
                .contentType("application/json")
                .content("""
                        {
                          "ownerId": null,
                          "currency": "INVALID"
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnEmptyTransactionHistoryForNewWallet() throws Exception {
        Wallet wallet = walletRepository.saveAndFlush(
                Wallet.create(UUID.randomUUID(), "USD"));

        mockMvc.perform(get(
                "/api/v1/wallets/{walletId}/transactions",
                wallet.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldReturnNotFoundForUnknownWalletTransactionHistory() throws Exception {
        mockMvc.perform(get(
                "/api/v1/wallets/{walletId}/transactions",
                UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnTransactionHistoryNewestFirst() throws Exception {
        Wallet wallet = walletRepository.saveAndFlush(
                Wallet.create(UUID.randomUUID(), "USD"));
        LedgerEntry oldest = ledgerEntry(wallet.getId(), "10.0000", 1);
        LedgerEntry newest = ledgerEntry(wallet.getId(), "30.0000", 3);
        LedgerEntry middle = ledgerEntry(wallet.getId(), "20.0000", 2);
        ledgerEntryRepository.saveAllAndFlush(List.of(oldest, newest, middle));

        mockMvc.perform(get(
                "/api/v1/wallets/{walletId}/transactions",
                wallet.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(newest.getId().toString()))
                .andExpect(jsonPath("$[1].id").value(middle.getId().toString()))
                .andExpect(jsonPath("$[2].id").value(oldest.getId().toString()))
                .andExpect(jsonPath("$[0].type").value("CREDIT"))
                .andExpect(jsonPath("$[0].amount").value(30.0000));
    }

    private LedgerEntry ledgerEntry(UUID walletId, String amount, int hour) {
        BigDecimal value = new BigDecimal(amount);
        return LedgerEntry.createAt(
                walletId,
                null,
                LedgerEntryType.CREDIT,
                value,
                value,
                OffsetDateTime.of(2026, 7, 25, hour, 0, 0, 0, ZoneOffset.UTC));
    }

    private static String extractJsonField(
            String json,
            String field) throws Exception {

        JsonNode root = new ObjectMapper()
                .readTree(json);

        return root.get(field).asString();
    }
}
