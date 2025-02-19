package com.company.app.ordermanager.messaging.common;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents a collection of channels used for message communication within the application.
 *
 * <ul>
 *   <li>{@link #STOCK_UPDATE_QUEUE} - Represents the stream used for stock update messages.</li>
 * </ul>
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum MessageChannels {
    STOCK_UPDATE_QUEUE("stock:update:stream");

    private final String key;
}
