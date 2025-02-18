package com.company.app.ordermanager.repository.api.order;

import com.company.app.ordermanager.entity.order.Order;
import com.company.app.ordermanager.entity.order.QOrder;
import com.querydsl.core.types.dsl.StringExpression;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;

import java.time.Instant;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, QuerydslPredicateExecutor<Order>, QuerydslBinderCustomizer<QOrder> {
    @Override
    default void customize(QuerydslBindings bindings, QOrder order) {
        /*
         * Configures case-insensitive string search for order description
         */
        bindings.bind(order.description).first((StringExpression::containsIgnoreCase));

        /*
         * Configures date search for order creation date
         * Supports both single date equality and date range searches
         */
        bindings.bind(order.createdAt).all(
                (path, values) -> {
                    Iterator<? extends Instant> it = values.iterator();

                    return switch (values.size()) {
                        case 1 -> Optional.of(path.eq(it.next()));
                        case 2 -> Optional.of(path.between(it.next(), it.next()));
                        default -> throw new IllegalArgumentException(
                                "Expected 1 date for equality or 2 dates for range search, got: " + values.size()
                        );
                    };
                }
        );
    }
}
