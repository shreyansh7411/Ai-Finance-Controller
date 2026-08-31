package com.aifincontroller.repository;

import com.aifincontroller.domain.ReconciliationResult;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReconciliationResultRepository extends JpaRepository<ReconciliationResult, Long> {

    List<ReconciliationResult> findByBatchId(String batchId);

    List<ReconciliationResult> findByPaymentReference(String paymentReference);
}
