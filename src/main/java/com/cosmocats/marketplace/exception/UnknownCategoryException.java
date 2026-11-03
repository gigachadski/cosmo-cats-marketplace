package com.cosmocats.marketplace.exception;

/** A request references a category that does not exist (the request itself is unprocessable). */
public class UnknownCategoryException extends RuntimeException {

    public UnknownCategoryException(Long id) {
        super("Category with id " + id + " does not exist");
    }
}
