package com.cosmocats.marketplace.persistence.repository;

import java.math.BigDecimal;

/** Interface projection: only these three columns are selected. */
public interface ProductSummaryView {

    Long getId();

    String getName();

    BigDecimal getPrice();
}
