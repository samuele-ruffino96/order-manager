package com.company.app.ordermanager.repository.api.orderitem;

import com.company.app.ordermanager.entity.orderitem.OrderItem;
import com.company.app.ordermanager.entity.orderitem.OrderItemStatus;
import com.company.app.ordermanager.entity.orderitem.OrderItemStatusReason;
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
            "AND oi.version = :version")
    int updateStatus(@Param("orderItemId") UUID orderItemId,
                     @Param("status") OrderItemStatus status,
                     @Param("version") long version);

    @Modifying
    @Query("UPDATE OrderItem oi SET oi.status = :status, oi.reason = :reason " +
            "WHERE oi.id = :orderItemId " +
            "AND oi.version = :version")
    int updateStatusAndReason(@Param("orderItemId") UUID orderItemId,
                              @Param("status") OrderItemStatus status,
                              @Param("version") long version,
                              @Param("reason") OrderItemStatusReason reason);

    @Query("SELECT oi.version FROM OrderItem oi WHERE oi.id = :orderItemId")
    Optional<Long> findVersionById(@Param("orderItemId") UUID orderItemId);
}
