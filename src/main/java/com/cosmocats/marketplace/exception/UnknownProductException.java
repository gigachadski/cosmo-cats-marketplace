package com.cosmocats.marketplace.exception;

/** An order references a product that does not exist (the request itself is unprocessable). */
public class UnknownProductException extends RuntimeException {

    public UnknownProductException(Long id) {
        super("Product with id " + id + " does not exist");
    }
}
