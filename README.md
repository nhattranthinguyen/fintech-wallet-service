# Fintech Wallet Service

A Java 17 and Spring Boot service for creating wallets and moving money safely. Transfers are atomic, idempotent, concurrency-safe, and recorded as balanced debit and credit ledger entries.

## Run from a clean checkout

Requirements: Docker with Docker Compose. No local Java, Maven, or PostgreSQL installation is required.

```bash
git clone <repository-url>
cd wallet-service
docker compose up --build
```

The Compose stack builds the application, starts PostgreSQL, waits for the database health check, and then starts the API. Flyway applies the schema automatically.

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI document: `http://localhost:8080/v3/api-docs`
- Health check: `http://localhost:8080/actuator/health`

By default, Compose creates `wallet_db` with the local credentials `wallet_user` / `wallet_password`. To override them, copy `.env.example` to `.env`, replace every placeholder value, and then start the stack. The local Spring profile uses the same `POSTGRES_*` variables.

Stop the services while preserving database data:

```bash
docker compose down
```

Remove the development database volume as well:

```bash
docker compose down --volumes
```

## API examples

Create a wallet:

```bash
curl -i -X POST http://localhost:8080/api/v1/wallets \
  -H "Content-Type: application/json" \
  -d '{"ownerId":"22222222-2222-2222-2222-222222222222","currency":"USD"}'
```

Get a wallet and its current balance:

```bash
curl http://localhost:8080/api/v1/wallets/{walletId}
```

Get its ledger-backed transaction history, newest first:

```bash
curl http://localhost:8080/api/v1/wallets/{walletId}/transactions
```

Create an idempotent transfer:

```bash
curl -i -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: order-2026-0001" \
  -d '{"sourceWalletId":"11111111-1111-1111-1111-111111111111","destinationWalletId":"33333333-3333-3333-3333-333333333333","amount":25.0000}'
```

Retrying that exact request with the same key returns the original transfer and does not move money again. Reusing the key with different request data returns `409 Conflict`.

## Architecture

Code is grouped by business capability (`wallet`, `transfer`, and `ledger`), then separated into four layers:

- `api`: HTTP contracts, validation, OpenAPI documentation, and error translation.
- `application`: transactional use-case orchestration.
- `domain`: wallet, transfer, and ledger rules without HTTP concerns.
- `infrastructure`: Spring Data JPA repositories and PostgreSQL coordination.

Flyway owns the database schema. Hibernate runs with `ddl-auto: validate`, so startup fails if entity mappings drift from the migrated schema.

The ledger is the financial record of every successful transfer. The wallet balance column is a transactionally maintained projection used to lock and validate funds efficiently; every change is accompanied by a ledger entry containing `balance_after`, making the projection reconcilable from the ledger.

## Correctness guarantees

### Idempotency

`TransferController` accepts an optional `Idempotency-Key`. `TransferService.execute(command, key)`:

1. hashes the canonical source, destination, and amount;
2. acquires a PostgreSQL transaction-scoped advisory lock for the operation and key;
3. looks up the key in `idempotency_keys`;
4. returns the stored transfer for the same request hash;
5. rejects a different request hash with `409 Conflict`; or
6. executes the transfer and stores the key and resulting transfer in the same transaction.

The database unique constraint on `(idempotency_key, operation_type)` is the final integrity boundary. The integration test `shouldReturnExistingTransferForRepeatedIdempotencyKey` proves that a retry creates one transfer and two ledger entries.

### Overdraw prevention

`WalletRepository.findByIdForUpdate` uses `PESSIMISTIC_WRITE`. `TransferService` locks both wallets in deterministic UUID order before checking or updating balances. The order avoids lock inversion, and the source row lock makes simultaneous withdrawals serialize. PostgreSQL also enforces `balance >= 0`.

`TransferConcurrencyIT` starts two competing withdrawals against a real PostgreSQL Testcontainer and proves that only one succeeds.

### Atomicity and ledger balance

Wallet updates, the transfer record, both ledger entries, and an idempotency record are written inside one Spring transaction. A debit and equal credit are created together. `TransferRollbackIT` forces the ledger insert to fail and verifies that neither wallet balances nor transfer data are committed.

## Error model

Errors use one JSON structure:

```json
{
  "timestamp": "2026-07-25T03:34:10Z",
  "status": 409,
  "error": "Conflict",
  "message": "Insufficient balance",
  "errors": null
}
```

Validation failures populate `errors` with messages keyed by field name. Expected status codes include `400` for invalid input, `404` for missing wallets, `409` for business or idempotency conflicts, and `500` for unexpected failures.

## Logging

The production profile emits Logstash-compatible JSON to standard output. Transfer completion, idempotent replay, and idempotency conflict logs include structured fields such as transfer ID, wallet IDs, request hash, amount, and currency. Container platforms can collect these logs without parsing human-formatted text.

## Tests

Docker must be running because integration tests use PostgreSQL Testcontainers.

```bash
./mvnw verify
```

Maven Surefire runs unit tests and Maven Failsafe runs integration tests. The suite covers domain rules, repositories, controllers, idempotency, concurrent withdrawals, and transaction rollback.

## Trade-offs and next steps

With more time, I would add a reconciliation job that periodically derives each wallet balance from immutable ledger entries and alerts on any difference from the materialized balance. I would also add pagination to transaction history before allowing unbounded account activity.

The optional outbox is intentionally not implemented yet. A useful outbox requires an event publisher, delivery retry policy, observability, and a concrete consumer contract; adding only an outbox table would imply reliability that the service does not yet deliver. The next increment would insert a `TransferCompleted` outbox row in the transfer transaction and publish it asynchronously with retry and delivery-state tracking.

## Adding a NOT NULL column with zero downtime

Use an expand-and-contract migration:

1. add the column as nullable, with no table-rewriting default;
2. deploy application code that writes both old and new representations;
3. backfill existing rows in small, restartable batches;
4. validate that no nulls remain and add a `NOT VALID` check constraint where PostgreSQL permits;
5. validate the constraint online;
6. set the column `NOT NULL` in a later migration; and
7. remove compatibility code only after every application instance uses the new schema.

This keeps old and new application versions compatible throughout a rolling deployment and avoids a long blocking rewrite.
