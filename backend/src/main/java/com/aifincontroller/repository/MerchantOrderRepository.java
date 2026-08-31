package com.aifincontroller.repository;

import com.aifincontroller.domain.MerchantOrder;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MerchantOrderRepository extends JpaRepository<MerchantOrder, Long> {

    Optional<MerchantOrder> findByOrderId(String orderId);

    boolean existsByOrderId(String orderId);
}
