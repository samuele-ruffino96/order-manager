package com.company.app.ordermanager.redis.stream.common;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents a collection of predefined Redis stream names used in the application for publishing
 * and processing messages. Each constant in this enum represents a specific stream name tied to
 * a distinct functionality such as stock updates, product updates, or order confirmations.
 *
 * <ul>
 *   <li>{@link #STOCK_UPDATE} - Represents the stream used for stock update messages.</li>
 *   <li>{@link #PRODUCT_UPDATE} - Represents the stream used for product update messages.</li>
 *   <li>{@link #ORDER_UPDATE} - Represents the stream used for order update messages.</li>
 * </ul>
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum StreamNames {
    STOCK_UPDATE("stock:update:stream"),
    PRODUCT_UPDATE("product:update:stream"),
    ORDER_UPDATE("order:update:stream");

    private final String key;
}
