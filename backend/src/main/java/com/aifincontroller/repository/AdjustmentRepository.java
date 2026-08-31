package com.aifincontroller.repository;

import com.aifincontroller.domain.Adjustment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdjustmentRepository extends JpaRepository<Adjustment, Long> {

    Optional<Adjustment> findByAdjustmentId(String adjustmentId);

    boolean existsByAdjustmentId(String adjustmentId);

    List<Adjustment> findBySettlementId(String settlementId);
}
