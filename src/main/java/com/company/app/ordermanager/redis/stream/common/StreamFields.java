package com.company.app.ordermanager.redis.stream.common;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum StreamFields {
    MESSAGE("message");

    private final String field;
}
