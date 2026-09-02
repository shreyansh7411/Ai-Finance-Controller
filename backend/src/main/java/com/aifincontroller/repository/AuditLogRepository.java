package com.aifincontroller.repository;

import com.aifincontroller.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtAsc(
            String entityType,
            String entityId
    );

    @Query("""
            SELECT a
            FROM AuditLog a
            WHERE a.entityId = :entityId
              AND a.entityType IN ('RECONCILIATION_EXCEPTION', 'DECISION')
            ORDER BY a.createdAt ASC, a.id ASC
            """)
    List<AuditLog> findExceptionAuditHistory(String entityId);
}
