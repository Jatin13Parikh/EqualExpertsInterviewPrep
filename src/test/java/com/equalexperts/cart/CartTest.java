package com.equalexperts.cart;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartTest {

    private static final Map<String, BigDecimal> STUB_PRICES = Map.of(
            "cornflakes", new BigDecimal("2.52"),
            "weetabix", new BigDecimal("9.98")
    );

    private PriceLookup stubPriceLookup;
    private Cart cart;

    @BeforeEach
    void setUp() {
        stubPriceLookup = productName -> {
            BigDecimal price = STUB_PRICES.get(productName);
            if (price == null) {
                throw new IllegalArgumentException("Unknown product: " + productName);
            }
            return price;
        };
        cart = new Cart(stubPriceLookup);
    }

    @Nested
    @DisplayName("when the cart is empty")
    class WhenEmpty {

        @Test
        void totalsAreZero() {
            assertAll(
                    () -> assertEquals(new BigDecimal("0.00"), cart.subtotal()),
                    () -> assertEquals(new BigDecimal("0.00"), cart.tax()),
                    () -> assertEquals(new BigDecimal("0.00"), cart.total())
            );
        }

        @Test
        void itemsReturnsEmptyList() {
            assertTrue(cart.items().isEmpty());
        }
    }

    @Nested
    @DisplayName("when adding a single product")
    class WhenAddingSingleProduct {

        @Test
        void addsItemWithCorrectQuantityAndPrice() {
            cart.addProduct("cornflakes", 1);

            List<CartItem> items = cart.items();
            assertEquals(1, items.size());
            assertEquals(new CartItem("cornflakes", 1, new BigDecimal("2.52")), items.get(0));
        }
    }

    @Nested
    @DisplayName("when adding the same product twice")
    class WhenAddingSameProductTwice {

        @Test
        void mergesQuantityAndKeepsOriginalUnitPrice() {
            cart.addProduct("cornflakes", 1);
            cart.addProduct("cornflakes", 1);

            assertEquals(1, cart.items().size());
            assertEquals(new CartItem("cornflakes", 2, new BigDecimal("2.52")), cart.items().get(0));
        }

        @Test
        void fetchesPriceOnlyOnFirstAdd() {
            AtomicInteger lookupCount = new AtomicInteger();
            Map<String, BigDecimal> prices = new HashMap<>(STUB_PRICES);
            Cart countingCart = new Cart(productName -> {
                lookupCount.incrementAndGet();
                return prices.get(productName);
            });

            countingCart.addProduct("cornflakes", 1);
            countingCart.addProduct("cornflakes", 1);

            assertEquals(1, lookupCount.get());
        }
    }

    @Nested
    @DisplayName("when calculating totals")
    class WhenCalculatingTotals {

        @Test
        void workedExampleProducesCorrectTotals() {
            cart.addProduct("cornflakes", 1);
            cart.addProduct("cornflakes", 1);
            cart.addProduct("weetabix", 1);

            assertAll(
                    () -> assertEquals(new BigDecimal("15.02"), cart.subtotal()),
                    () -> assertEquals(new BigDecimal("1.88"), cart.tax()),
                    () -> assertEquals(new BigDecimal("16.90"), cart.total())
            );
        }

        @Test
        void roundsTaxUpToTwoDecimalPlaces() {
            Cart taxCart = new Cart(productName -> new BigDecimal("10.01"));

            taxCart.addProduct("cheerios", 1);

            assertEquals(new BigDecimal("10.01"), taxCart.subtotal());
            assertEquals(new BigDecimal("1.26"), taxCart.tax());
            assertEquals(new BigDecimal("11.27"), taxCart.total());
        }
    }

    @Nested
    @DisplayName("when validating input")
    class WhenValidatingInput {

        @Test
        void rejectsZeroQuantity() {
            assertThrows(IllegalArgumentException.class, () -> cart.addProduct("cornflakes", 0));
        }

        @Test
        void rejectsNegativeQuantity() {
            assertThrows(IllegalArgumentException.class, () -> cart.addProduct("cornflakes", -1));
        }

        @Test
        void rejectsBlankProductName() {
            assertThrows(IllegalArgumentException.class, () -> cart.addProduct("  ", 1));
        }
    }

    @Nested
    @DisplayName("when exposing cart state")
    class WhenExposingCartState {

        @Test
        void returnsItemsListThatIsUnmodifiable() {
            cart.addProduct("cornflakes", 1);

            List<CartItem> items = cart.items();
            assertThrows(UnsupportedOperationException.class, () -> items.add(
                    new CartItem("weetabix", 1, new BigDecimal("9.98"))
            ));
        }

        @Test
        void returnsSnapshotThatDoesNotReflectSubsequentMutations() {
            cart.addProduct("cornflakes", 1);
            List<CartItem> snapshot = cart.items();

            cart.addProduct("weetabix", 1);

            assertEquals(1, snapshot.size());
            assertEquals(2, cart.items().size());
        }
    }
}
