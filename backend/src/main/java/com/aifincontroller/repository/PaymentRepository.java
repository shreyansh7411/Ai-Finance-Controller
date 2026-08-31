package com.aifincontroller.repository;

import com.aifincontroller.domain.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentId(String paymentId);

    boolean existsByPaymentId(String paymentId);

    List<Payment> findByOrderId(String orderId);
}
