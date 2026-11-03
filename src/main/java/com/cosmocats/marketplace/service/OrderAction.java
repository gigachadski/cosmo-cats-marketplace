package com.cosmocats.marketplace.service;

import com.cosmocats.marketplace.domain.Order;

import java.util.Locale;
import java.util.function.UnaryOperator;

/** Status changes a client may request; each one delegates to the domain rule on {@link Order}. */
public enum OrderAction {
    PAY(Order::pay),
    SHIP(Order::ship),
    DELIVER(Order::deliver),
    CANCEL(Order::cancel);

    private final UnaryOperator<Order> transition;

    OrderAction(UnaryOperator<Order> transition) {
        this.transition = transition;
    }

    public Order applyTo(Order order) {
        return transition.apply(order);
    }

    /** Parses the path segment of {@code POST /orders/{number}/{action}}, e.g. "pay". */
    public static OrderAction fromPath(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
