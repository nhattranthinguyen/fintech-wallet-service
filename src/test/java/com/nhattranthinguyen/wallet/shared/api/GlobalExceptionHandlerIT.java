package com.nhattranthinguyen.wallet.shared.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.nhattranthinguyen.wallet.wallet.application.exception.WalletAlreadyExistsException;
import com.nhattranthinguyen.wallet.wallet.application.exception.WalletNotFoundException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@WebMvcTest(GlobalExceptionHandlerIT.TestController.class)
@Import({
        GlobalExceptionHandler.class,
        GlobalExceptionHandlerIT.TestController.class
})
class GlobalExceptionHandlerIT {
    private static final UUID WALLET_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID OWNER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnNotFoundWhenWalletDoesNotExist() throws Exception {
        mockMvc.perform(get("/test/errors/wallet-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value(containsString(WALLET_ID.toString())));
    }

    @Test
    void shouldReturnConflictWhenWalletAlreadyExists() throws Exception {
        mockMvc.perform(get("/test/errors/wallet-already-exists"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value(containsString(OWNER_ID.toString())))
                .andExpect(jsonPath("$.message")
                        .value(containsString("USD")));
    }

    @Test
    void shouldReturnInternalServerErrorForUnexpectedException()
            throws Exception {

        mockMvc.perform(get("/test/errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    void shouldReturnBadRequestWhenRequestBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/test/errors/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                        "currency": ""
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Validation failed"))
                .andExpect(jsonPath("$.errors.currency")
                        .value("Currency is required"));
    }

    @RestController
    public static class TestController {
        @GetMapping("/test/errors/wallet-not-found")
        void walletNotFound() {
            throw new WalletNotFoundException(WALLET_ID);
        }

        @GetMapping("/test/errors/wallet-already-exists")
        void walletAlreadyExists() {
            throw new WalletAlreadyExistsException(
                    OWNER_ID,
                    "USD");
        }

        @GetMapping("/test/errors/unexpected")
        void unexpectedException() {
            throw new IllegalStateException("Unexpected test failure");
        }

        @PostMapping("/test/errors/validation")
        void validation(
                @Valid @RequestBody ValidationRequest request) {
        }
    }

    record ValidationRequest(
            @NotBlank(message = "Currency is required")
            @Pattern(
                    regexp = "^[A-Z]{3}$",
                    message = "Currency must be a 3-letter uppercase code"
            )
            String currency
    ) {
    }
}
