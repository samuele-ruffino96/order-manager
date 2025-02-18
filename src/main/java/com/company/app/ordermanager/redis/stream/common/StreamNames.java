package com.company.app.ordermanager.redis.stream.common;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents a collection of predefined Redis stream names used in the application for publishing
 * and processing messages. Each constant in this enum represents a specific stream name tied to
 * a distinct functionality such as stock updates.
 *
 * <ul>
 *   <li>{@link #STOCK_UPDATE_QUEUE} - Represents the stream used for stock update messages.</li>
 * </ul>
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum StreamNames {
    STOCK_UPDATE_QUEUE("stock:update:stream");

    private final String key;
}
