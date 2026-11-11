package com.cosmocats.marketplace.exception;

import com.cosmocats.marketplace.domain.OrderStatus;

/** The requested status change is not allowed by the order lifecycle. */
public class InvalidOrderStatusTransitionException extends IllegalStateException {

    public InvalidOrderStatusTransitionException(OrderStatus from, OrderStatus to) {
        super("Cannot change order status from " + from + " to " + to);
    }
}
