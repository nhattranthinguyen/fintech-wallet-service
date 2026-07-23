package com.nhattranthinguyen.wallet.wallet.integration;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.nhattranthinguyen.wallet.support.PostgresIT;
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

    @BeforeEach
    void cleanDatabase() {
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

    private static String extractJsonField(
            String json,
            String field) throws Exception {

        JsonNode root = new ObjectMapper()
                .readTree(json);

        return root.get(field).asString();
    }
}
