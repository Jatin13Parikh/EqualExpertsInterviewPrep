package com.equalexperts.cart;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Cart {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.125");
    private static final int SCALE = 2;

    private final Map<String, CartItem> items = new LinkedHashMap<>();
    private final PriceLookup priceLookup;

    public Cart(PriceLookup priceLookup) {
        if (priceLookup == null) {
            throw new IllegalArgumentException("Price lookup must not be null");
        }
        this.priceLookup = priceLookup;
    }

    public void addProduct(String name, int quantity) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        items.compute(name, (key, existing) -> {
            if (existing == null) {
                return new CartItem(name, quantity, priceLookup.getPrice(name));
            }
            return new CartItem(name, existing.quantity() + quantity, existing.unitPrice());
        });
    }

    public List<CartItem> items() {
        return List.copyOf(items.values());
    }

    public BigDecimal subtotal() {
        return items.values().stream()
                .map(CartItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, RoundingMode.UNNECESSARY);
    }

    public BigDecimal tax() {
        return roundUp(subtotal().multiply(TAX_RATE));
    }

    public BigDecimal total() {
        return subtotal().add(tax());
    }

    private static BigDecimal roundUp(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.UP);
    }
}
