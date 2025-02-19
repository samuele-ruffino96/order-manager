package com.company.app.ordermanager.messaging.redis;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents predefined field names used in Redis stream entries within the application.
 * Each constant serves as a unique identifier for specific fields present in the stream messages.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum StreamFields {
    MESSAGE("message");

    private final String field;
}
