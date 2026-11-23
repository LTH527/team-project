package data_access.alpaca;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AlpacaWebSocketClient {

    private static final String WEBSOCKET_URL =
            "wss://stream.data.alpaca.markets/v2/iex";

    private final String apiKey;
    private final String apiSecret;
    private final OkHttpClient httpClient;
    private final Map<String, CachedPrice> latestPrices =
            new ConcurrentHashMap<>();
    private final Set<String> subscribedTickers =
            ConcurrentHashMap.newKeySet();

    private volatile WebSocket webSocket;
    private volatile boolean authenticated;
    private volatile boolean closed;
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean(false);
    private volatile BiConsumer<String, BigDecimal> priceUpdateListener =
            (ticker, price) -> { };
    private final ScheduledExecutorService reconnectExecutor =
            Executors.newSingleThreadScheduledExecutor();

    public AlpacaWebSocketClient(String apiKey, String apiSecret) {
        this(apiKey, apiSecret, new OkHttpClient());
    }

    public AlpacaWebSocketClient(String apiKey, String apiSecret,
                                 OkHttpClient httpClient) {
        this.apiKey = requireCredential(apiKey, "APCA_API_KEY_ID");
        this.apiSecret = requireCredential(apiSecret, "APCA_API_SECRET_KEY");
        this.httpClient = Objects.requireNonNull(httpClient);
        connect();
    }

    public void subscribe(String ticker) {
        String normalized = ticker.trim().toUpperCase(Locale.ROOT);
        if (!subscribedTickers.add(normalized)) {
            return;
        }

        WebSocket socket = webSocket;
        if (socket != null && authenticated) {
            JSONObject message = new JSONObject();
            message.put("action", "subscribe");
            message.put("trades", new JSONArray().put(normalized));
            socket.send(message.toString());
        }
    }

    public BigDecimal getLatestPrice(String ticker) {
        CachedPrice cached = latestPrices.get(
                ticker.trim().toUpperCase(Locale.ROOT));
        return cached == null ? null : cached.getPrice();
    }

    public void setLatestPrice(String ticker, BigDecimal price, Instant timestamp) {
        latestPrices.compute(ticker.trim().toUpperCase(Locale.ROOT),
                (key, existing) -> existing == null
                        || timestamp.compareTo(existing.getTimestamp()) >= 0
                        ? new CachedPrice(price, timestamp)
                        : existing);
    }

    public void setPriceUpdateListener(
            BiConsumer<String, BigDecimal> priceUpdateListener) {
        this.priceUpdateListener = Objects.requireNonNull(
                priceUpdateListener, "priceUpdateListener must not be null");
    }

    public void close() {
        closed = true;
        WebSocket socket = webSocket;
        if (socket != null) {
            socket.close(1000, "Portfolio view closed");
        }
        reconnectExecutor.shutdownNow();
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }

    private void connect() {
        Request request = new Request.Builder()
                .url(WEBSOCKET_URL)
                .build();
        webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket socket, Response response) {
                JSONObject auth = new JSONObject();
                auth.put("action", "auth");
                auth.put("key", apiKey);
                auth.put("secret", apiSecret);
                socket.send(auth.toString());
            }

            @Override
            public void onMessage(WebSocket socket, String text) {
                try {
                    JSONArray messages = new JSONArray(text);
                    for (int index = 0; index < messages.length(); index++) {
                        JSONObject message = messages.getJSONObject(index);
                        String type = message.optString("T");
                        if ("success".equals(type)
                                && "authenticated".equals(message.optString("msg"))) {
                            authenticated = true;
                            subscribeAll(socket);
                        } else if ("subscription".equals(type)) {
                            // The subscription acknowledgement confirms the requested feed.
                        } else if ("t".equals(type)) {
                            String ticker = message.optString("S")
                                    .trim().toUpperCase(Locale.ROOT);
                            if (AlpacaPriceParser.updateTradePrices(
                                    message, latestPrices)
                                    && !ticker.isEmpty()) {
                                BigDecimal price = getLatestPrice(ticker);
                                if (price != null) {
                                    priceUpdateListener.accept(ticker, price);
                                }
                            }
                        } else if ("error".equals(type)) {
                            System.err.println(
                                    "Alpaca WebSocket error: "
                                            + message.optString("msg", "Unknown error"));
                        }
                    }
                } catch (RuntimeException ignored) {
                    // Keep the last valid cached prices on malformed feed messages.
                }
            }

            @Override
            public void onMessage(WebSocket socket, ByteString bytes) {
                onMessage(socket, bytes.utf8());
            }

            @Override
            public void onClosing(WebSocket socket, int code, String reason) {
                socket.close(code, reason);
                authenticated = false;
                if (!closed) {
                    scheduleReconnect();
                }
            }

            @Override
            public void onFailure(WebSocket socket, Throwable t, Response response) {
                authenticated = false;
                scheduleReconnect();
            }
        });
    }

    private void scheduleReconnect() {
        if (closed || !reconnectScheduled.compareAndSet(false, true)) {
            return;
        }
        reconnectExecutor.schedule(() -> {
            reconnectScheduled.set(false);
            if (!closed) {
                connect();
            }
        }, 2, TimeUnit.SECONDS);
    }

    private void subscribeAll(WebSocket socket) {
        if (subscribedTickers.isEmpty()) {
            return;
        }

        JSONObject message = new JSONObject();
        message.put("action", "subscribe");
        message.put("trades", new JSONArray(subscribedTickers));
        socket.send(message.toString());
    }

    private static String requireCredential(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Set " + name + " and APCA_API_SECRET_KEY in your run configuration.");
        }
        return value.trim();
    }

    public static final class CachedPrice {
        private final BigDecimal price;
        private final Instant timestamp;

        public CachedPrice(BigDecimal price, Instant timestamp) {
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
