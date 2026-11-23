package data_access.alpaca;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

public final class AlpacaPriceParser {

    private AlpacaPriceParser() {
    }

    public static boolean updateTradePrices(
            JSONObject message,
            Map<String, AlpacaWebSocketClient.CachedPrice> prices) {
        if (!message.has("T") || !"t".equals(message.optString("T"))) {
            return false;
        }

        String ticker = message.optString("S").trim().toUpperCase(Locale.ROOT);
        if (ticker.isEmpty() || !message.has("p")) {
            return false;
        }

        try {
            BigDecimal price = new BigDecimal(message.opt("p").toString());
            if (price.signum() > 0) {
                Instant timestamp = parseTimestamp(message.optString("t"));
                final boolean[] updated = {false};
                prices.compute(ticker, (key, existing) -> {
                    if (existing == null
                            || timestamp.compareTo(existing.getTimestamp()) > 0) {
                        updated[0] = true;
                        return new AlpacaWebSocketClient.CachedPrice(price, timestamp);
                    }
                    return existing;
                });
                return updated[0];
            }
        } catch (RuntimeException ignored) {
            // Ignore malformed trade events and keep the last valid price.
        }
        return false;
    }

    public static void updateTradePrices(
            String body,
            Map<String, AlpacaWebSocketClient.CachedPrice> prices) {
        try {
            JSONArray messages = new JSONArray(body);
            for (int index = 0; index < messages.length(); index++) {
                updateTradePrices(messages.getJSONObject(index), prices);
            }
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Malformed Alpaca WebSocket message", exception);
        }
    }

    public static AlpacaSnapshot parseSnapshot(String body, String ticker)
            throws IOException {
        try {
            JSONObject root = new JSONObject(body);
            if (!root.has(ticker)) {
                throw new IOException("No Alpaca snapshot was returned for " + ticker + ".");
            }

            JSONObject snapshot = root.getJSONObject(ticker);
            JSONObject latestTrade = snapshot.getJSONObject("latestTrade");
            BigDecimal price = latestTrade.getBigDecimal("p");
            if (price.signum() <= 0) {
                throw new IOException("Alpaca returned a nonpositive price.");
            }
            Instant timestamp = parseTimestamp(latestTrade.optString("t"));
            return new AlpacaSnapshot(price, timestamp);
        } catch (IOException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IOException("Alpaca returned malformed snapshot data.");
        }
    }

    private static Instant parseTimestamp(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Instant.EPOCH;
        }
        try {
            return Instant.parse(value);
        } catch (RuntimeException exception) {
            return Instant.EPOCH;
        }
    }

    public static final class AlpacaSnapshot {
        private final BigDecimal price;
        private final Instant timestamp;

        private AlpacaSnapshot(BigDecimal price, Instant timestamp) {
            this.price = price;
            this.timestamp = timestamp;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public Instant getTimestamp() {
            return timestamp;
        }
    }
}
