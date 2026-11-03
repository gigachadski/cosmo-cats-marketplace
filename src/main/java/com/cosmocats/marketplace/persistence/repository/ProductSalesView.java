package com.cosmocats.marketplace.persistence.repository;

import java.math.BigDecimal;

/** Interface projection for the top-selling products report. */
public interface ProductSalesView {

    Long getProductId();

    String getProductName();

    Long getTotalQuantity();

    BigDecimal getTotalRevenue();
}
