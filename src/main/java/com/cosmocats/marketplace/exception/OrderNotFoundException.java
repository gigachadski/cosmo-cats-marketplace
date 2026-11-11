package com.cosmocats.marketplace.exception;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(UUID orderNumber) {
        super("Order " + orderNumber + " was not found");
    }
}
