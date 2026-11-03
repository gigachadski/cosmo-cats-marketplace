package com.cosmocats.marketplace.exception;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(Long id) {
        super("Category with id " + id + " does not exist");
    }
}
