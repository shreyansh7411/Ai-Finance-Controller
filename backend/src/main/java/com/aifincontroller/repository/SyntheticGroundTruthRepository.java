package com.aifincontroller.repository;

import com.aifincontroller.domain.SyntheticGroundTruth;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SyntheticGroundTruthRepository
        extends JpaRepository<SyntheticGroundTruth, Long> {

    List<SyntheticGroundTruth> findByBatchId(String batchId);

    List<SyntheticGroundTruth> findByScenario(
            com.aifincontroller.domain.SyntheticScenario scenario);
}
