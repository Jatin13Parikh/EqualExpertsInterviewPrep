package com.equalexperts.price;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PriceApiClientTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        int port = server.getAddress().getPort();
        baseUrl = "http://localhost:" + port + "/";
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Nested
    @DisplayName("when the API returns a valid response")
    class WhenApiReturnsValidResponse {

        @Test
        void returnsPriceForProduct() {
            server.createContext("/cornflakes.json", exchange -> {
                byte[] body = "{\"title\":\"Corn Flakes\",\"price\":2.52}".getBytes();
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            PriceApiClient client = new PriceApiClient(HttpClient.newHttpClient(), baseUrl);

            assertEquals(new BigDecimal("2.52"), client.getPrice("cornflakes"));
        }
    }

    @Nested
    @DisplayName("when the API returns an error")
    class WhenApiReturnsError {

        @Test
        void throwsWhenStatusIsNot200() {
            server.createContext("/unknown.json", exchange -> {
                byte[] body = "not found".getBytes();
                exchange.sendResponseHeaders(404, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            PriceApiClient client = new PriceApiClient(HttpClient.newHttpClient(), baseUrl);

            assertThrows(PriceApiException.class, () -> client.getPrice("unknown"));
        }

        @Test
        void throwsWhenResponseHasNoPriceField() {
            server.createContext("/broken.json", exchange -> {
                byte[] body = "{\"title\":\"Broken\"}".getBytes();
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            PriceApiClient client = new PriceApiClient(HttpClient.newHttpClient(), baseUrl);

            assertThrows(PriceApiException.class, () -> client.getPrice("broken"));
        }
    }

    @Nested
    @DisplayName("when parsing JSON")
    class WhenParsingJson {

        @Test
        void parsesPriceFromTypicalResponse() {
            assertEquals(
                    new BigDecimal("9.98"),
                    PriceApiClient.parsePrice("{\"title\":\"Weetabix\",\"price\":9.98}")
            );
        }
    }

    @Nested
    @Tag("integration")
    @DisplayName("integration with live Price API")
    class IntegrationWithLiveApi {

        @Test
        void fetchesCornflakesPriceFromLiveApi() {
            PriceApiClient client = new PriceApiClient();

            assertEquals(new BigDecimal("2.52"), client.getPrice("cornflakes"));
        }
    }
}
