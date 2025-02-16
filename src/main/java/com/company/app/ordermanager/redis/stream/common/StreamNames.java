package com.company.app.ordermanager.redis.stream.common;

public enum StreamNames {
    ORDER_RESERVATION("order:reservation:stream"),
    ORDER_CONFIRMATION("order:confirmation:stream"),
    STOCK_UPDATE("stock:update:stream");

    private final String key;

    StreamNames(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
