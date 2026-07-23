package com.nhattranthinguyen.wallet.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
public abstract class PostgresIT {
    @SuppressWarnings("resource")
    @Container
    protected static final PostgreSQLContainer postgres =
        new PostgreSQLContainer("postgres:18")
            .withDatabaseName("wallet_test")
            .withUsername("wallet_test")
            .withPassword("wallet_test");

    @DynamicPropertySource
    static void configurePostgres(
        DynamicPropertyRegistry registry
    ) {
        registry.add(
            "spring.datasource.url",
            postgres::getJdbcUrl
        );
        registry.add(
            "spring.datasource.username",
            postgres::getUsername
        );
        registry.add(
            "spring.datasource.password",
            postgres::getPassword
        );
        registry.add(
            "spring.jpa.hibernate.ddl-auto",
            () -> "validate"
        );
        registry.add(
            "spring.flyway.enabled",
            () -> "true"
        );
    }
}
