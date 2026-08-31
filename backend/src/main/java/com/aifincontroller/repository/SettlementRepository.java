package com.aifincontroller.repository;

import com.aifincontroller.domain.Settlement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    List<Settlement> findBySettlementId(String settlementId);

    List<Settlement> findByPaymentId(String paymentId);

    Optional<Settlement> findBySettlementIdAndPaymentId(String settlementId, String paymentId);

    List<Settlement> findByUtr(String utr);
}
