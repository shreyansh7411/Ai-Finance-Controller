package com.aifincontroller.repository;

import com.aifincontroller.domain.ReconciliationException;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReconciliationExceptionRepository
        extends JpaRepository<ReconciliationException, Long> {

    List<ReconciliationException> findByReconciliationResultId(
            Long reconciliationResultId);
}
