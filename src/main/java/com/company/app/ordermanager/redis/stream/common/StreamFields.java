package com.company.app.ordermanager.redis.stream.common;

public enum StreamFields {
    MESSAGE("message"),
    ERROR("error"),
    STATUS("status");

    private final String field;

    StreamFields(String field) {
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
