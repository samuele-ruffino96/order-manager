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
 *   <li>{@link #STOCK_UPDATE_QUEUE} - Represents the stream used for stock update messages.</li>
 *   <li>{@link #PRODUCT_STOCK_LEVEL_UPDATE_QUEUE} - Represents the stream used for product update messages.</li>
 *   <li>{@link #ORDER_ITEM_STATUS_UPDATE_QUEUE} - Represents the stream used for order items status update messages.</li>
 * </ul>
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum StreamNames {
    STOCK_UPDATE_QUEUE("stock:update:stream"),
    PRODUCT_STOCK_LEVEL_UPDATE_QUEUE("product:update:stream"),
    ORDER_ITEM_STATUS_UPDATE_QUEUE("order-item:update:stream");

    private final String key;
}
