package com.cosmocats.marketplace.exception;

/** The exchange rate service does not know the requested currency. */
public class UnsupportedCurrencyException extends RuntimeException {

    public UnsupportedCurrencyException(String currency) {
        super("Currency " + currency + " is not supported by the exchange rate service");
    }
}
