package com.company.app.ordermanager.repository.api.orderitem;

import com.company.app.ordermanager.entity.orderitem.OrderItem;
import com.company.app.ordermanager.entity.orderitem.OrderItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    @Modifying
    @Query("UPDATE OrderItem oi SET oi.status = :status " +
            "WHERE oi.id = :orderItemId " +
            "AND oi.product.id = :productId " +
            "AND oi.order.id = :orderId")
    int updateStatus(@Param("orderItemId") UUID orderItemId,
                     @Param("productId") UUID productId,
                     @Param("orderId") UUID orderId,
                     @Param("status") OrderItemStatus status);

    @Modifying
    @Query("UPDATE OrderItem oi SET oi.status = :status, oi.error = :error " +
            "WHERE oi.id = :orderItemId " +
            "AND oi.product.id = :productId " +
            "AND oi.order.id = :orderId")
    int updateStatusAndError(@Param("orderItemId") UUID orderItemId,
                             @Param("productId") UUID productId,
                             @Param("orderId") UUID orderId,
                             @Param("status") OrderItemStatus status,
                             @Param("error") String error);

    @Query("SELECT oi.version FROM OrderItem oi WHERE oi.id = :orderItemId")
    Optional<Long> findVersionById(@Param("orderItemId") UUID orderItemId);
}
