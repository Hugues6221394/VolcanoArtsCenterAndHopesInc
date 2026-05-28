package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.OrderItem;
import com.volcanoartscenter.platform.shared.model.ShippingOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    long countByProduct_Id(Long productId);

    /**
     * Check if a user has purchased a specific product via a DELIVERED order.
     * Used for review eligibility enforcement per master spec.
     */
    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi " +
           "JOIN oi.order o " +
           "WHERE o.user.id = :userId " +
           "AND oi.product.id = :productId " +
           "AND o.status = com.volcanoartscenter.platform.shared.model.ShippingOrder.OrderStatus.DELIVERED")
    boolean hasUserPurchasedAndReceived(Long userId, Long productId);
}
