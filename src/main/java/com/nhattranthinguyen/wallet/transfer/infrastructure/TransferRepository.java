package com.nhattranthinguyen.wallet.transfer.infrastructure;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nhattranthinguyen.wallet.transfer.domain.Transfer;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {
}
