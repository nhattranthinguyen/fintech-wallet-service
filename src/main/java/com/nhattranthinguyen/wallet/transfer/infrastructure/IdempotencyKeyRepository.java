package com.nhattranthinguyen.wallet.transfer.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {
    Optional<IdempotencyKey> findByKeyAndOperationType(String key, String operationType);

    default Optional<IdempotencyKey> findTransferKey(String key) {
        return findByKeyAndOperationType(key, "CREATE_TRANSFER");
    }

    @Query(value = "select pg_advisory_xact_lock(hashtext(:lockKey))", nativeQuery = true)
    void lock(@Param("lockKey") String lockKey);
}
