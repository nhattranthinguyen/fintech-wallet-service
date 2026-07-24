package com.nhattranthinguyen.wallet.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI walletApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Wallet API")
                        .version("1.0.0")
                        .description("Senior Java Backend Architect Take-home Assignment"));
    }
}
