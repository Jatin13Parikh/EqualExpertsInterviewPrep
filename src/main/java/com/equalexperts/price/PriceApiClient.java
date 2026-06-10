package com.equalexperts.price;

import com.equalexperts.cart.PriceLookup;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class PriceApiClient implements PriceLookup {

    private static final String BASE_URL = "https://equalexperts.github.io/backend-take-home-test-data/";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final String baseUrl;

    public PriceApiClient() {
        this(HttpClient.newBuilder().connectTimeout(TIMEOUT).build(), BASE_URL);
    }

    PriceApiClient(HttpClient httpClient, String baseUrl) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
    }

    @Override
    public BigDecimal getPrice(String productName) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + productName + ".json"))
                    .timeout(TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new PriceApiException(
                        "Failed to fetch price for product '" + productName + "': HTTP " + response.statusCode()
                );
            }

            return parsePrice(response.body());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new PriceApiException("Failed to fetch price for product '" + productName + "'", e);
        }
    }

    static BigDecimal parsePrice(String json) {
        int priceIndex = json.indexOf("\"price\"");
        if (priceIndex < 0) {
            throw new PriceApiException("Price field not found in API response");
        }

        int colonIndex = json.indexOf(':', priceIndex);
        if (colonIndex < 0) {
            throw new PriceApiException("Invalid price field in API response");
        }

        int start = colonIndex + 1;
        while (start < json.length() && (Character.isWhitespace(json.charAt(start)) || json.charAt(start) == '"')) {
            start++;
        }

        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.')) {
            end++;
        }

        if (start == end) {
            throw new PriceApiException("Invalid price value in API response");
        }

        return new BigDecimal(json.substring(start, end));
    }
}
