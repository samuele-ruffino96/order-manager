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
    /**
     * Updates the status of an {@link OrderItem} in the database based on its unique identifier
     * and version. This method ensures that the update is atomic by considering the current
     * version of the {@link OrderItem}.
     *
     * @param orderItemId the unique identifier of the {@link OrderItem}.
     * @param status      the new status to set for the {@link OrderItem}.
     * @param version     the current version of the {@link OrderItem}.
     * @return the number of rows affected by the update.
     * @throws IllegalArgumentException if any parameter is invalid.
     */
    @Modifying
    @Query("UPDATE OrderItem oi SET oi.status = :status " +
            "WHERE oi.id = :orderItemId " +
            "AND oi.version = :version")
    int updateStatus(@Param("orderItemId") UUID orderItemId,
                     @Param("status") OrderItemStatus status,
                     @Param("version") long version);

    /**
     * Updates the status and reason of an {@link OrderItem} in the database based on its unique
     * identifier and version. Ensures the operation is atomic by considering the current version of
     * the {@link OrderItem}.
     *
     * @param orderItemId the unique identifier of the {@link OrderItem}.
     * @param status the new status to set for the {@link OrderItem}.
     * @param version the current version of the {@link OrderItem}.
     * @param reason the reason associated with the status change for the {@link OrderItem}.
     * @return the number of rows affected by the update.
     * @throws IllegalArgumentException if any parameter is invalid.
     */
    @Modifying
    @Query("UPDATE OrderItem oi SET oi.status = :status, oi.reason = :reason " +
            "WHERE oi.id = :orderItemId " +
            "AND oi.version = :version")
    int updateStatusAndReason(@Param("orderItemId") UUID orderItemId,
                              @Param("status") OrderItemStatus status,
                              @Param("version") long version,
                              @Param("reason") OrderItemStatusReason reason);

    /**
     * Retrieves the version of an {@link OrderItem} based on its unique identifier.
     *
     * @param orderItemId the unique identifier of the {@link OrderItem}.
     * @return an {@link Optional} containing the version of the {@link OrderItem} if found.
     * @throws IllegalArgumentException if the orderItemId is null.
     */
    @Query("SELECT oi.version FROM OrderItem oi WHERE oi.id = :orderItemId")
    Optional<Long> findVersionById(@Param("orderItemId") UUID orderItemId);
}
