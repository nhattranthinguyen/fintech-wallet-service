# Fintech Wallet Service
Production-oriented wallet and money transfer service

## Status

This project is under active development as part of a backend
architecture assessment.

## Technology

- Java 17
- Spring Boot
- PostgreSQL
- Flyway
- Maven
- JUnit 5
- Testcontainers

## Current Capabilities

- Create a wallet
- Query a wallet balance derived from ledger entries

## Architecture

The service uses a lightweight hexagonal structure organized by
business capability.

- API: HTTP transport and validation
- Application: use-case orchestration
- Domain: financial concepts and rules
- Infrastructure: PostgreSQL and JPA adapters

## Local Development

Instructions will be expanded as Docker Compose support is added.

## Planned Transfer Guarantee

Transfers will use a single PostgreSQL transaction, database-backed
idempotency, row-level wallet locking, and balanced debit/credit ledger
entries.