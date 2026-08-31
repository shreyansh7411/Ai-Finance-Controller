package com.aifincontroller.repository;

import com.aifincontroller.domain.Refund;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    Optional<Refund> findByRefundId(String refundId);

    boolean existsByRefundId(String refundId);

    List<Refund> findByPaymentId(String paymentId);
}
