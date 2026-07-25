package com.nhattranthinguyen.wallet.transfer.infrastructure;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "idempotency_keys",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_idempotency_key_operation",
        columnNames = {"idempotency_key", "operation_type"}
    )
)
public class IdempotencyKey {
    private static final String TRANSFER_OPERATION = "CREATE_TRANSFER";

    @Id
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String key;

    @Column(name = "operation_type", nullable = false, length = 100)
    private String operationType;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    protected IdempotencyKey() {
    }

    private IdempotencyKey(String key, String requestHash, UUID resourceId) {
        this.id = UUID.randomUUID();
        this.key = key;
        this.operationType = TRANSFER_OPERATION;
        this.requestHash = requestHash;
        this.resourceId = resourceId;
        this.responseStatus = 201;
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.expiresAt = createdAt.plusHours(24);
    }

    public static IdempotencyKey forTransfer(String key, String requestHash, UUID transferId) {
        return new IdempotencyKey(key, requestHash, transferId);
    }

    public String getRequestHash() {
        return requestHash;
    }

    public UUID getResourceId() {
        return resourceId;
    }
}
