package com.aifincontroller.repository;

import com.aifincontroller.domain.ReconciliationException;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ReconciliationExceptionRepository
        extends JpaRepository<ReconciliationException, Long> {

    List<ReconciliationException> findByReconciliationResultId(
            Long reconciliationResultId);

    List<ReconciliationException> findByReconciliationResultIdIn(
            Collection<Long> reconciliationResultIds);

    long countByStatusNot(String status);

    @Query("""
            SELECT e.category, COUNT(e)
            FROM ReconciliationException e
            GROUP BY e.category
            ORDER BY COUNT(e) DESC
            """)
    List<Object[]> countByCategory();
}
