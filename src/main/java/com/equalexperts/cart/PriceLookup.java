package com.equalexperts.cart;

import java.math.BigDecimal;

@FunctionalInterface
public interface PriceLookup {

    BigDecimal getPrice(String productName);
}
