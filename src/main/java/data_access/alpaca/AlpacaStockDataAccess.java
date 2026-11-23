package data_access.alpaca;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import stockprototype.StockSymbol;
import use_case.portfolio.StockPriceDataAccessInterface;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.BiConsumer;

public final class AlpacaStockDataAccess
        implements StockPriceDataAccessInterface, AutoCloseable {

    private static final String SNAPSHOT_URL =
            "https://data.alpaca.markets/v2/stocks/snapshots";

    private final String apiKey;
    private final String apiSecret;
    private final AlpacaWebSocketClient webSocketClient;
    private final OkHttpClient httpClient;
    private volatile BiConsumer<String, BigDecimal> priceUpdateListener =
            (ticker, price) -> { };

    public AlpacaStockDataAccess(String apiKey, String apiSecret) {
        this(apiKey, apiSecret, new OkHttpClient(),
                new AlpacaWebSocketClient(apiKey, apiSecret));
    }

    public AlpacaStockDataAccess(
            String apiKey,
            String apiSecret,
            OkHttpClient httpClient,
            AlpacaWebSocketClient webSocketClient) {
        this.apiKey = requireCredential(apiKey, "APCA_API_KEY_ID");
        this.apiSecret = requireCredential(apiSecret, "APCA_API_SECRET_KEY");
        this.httpClient = Objects.requireNonNull(httpClient);
        this.webSocketClient = Objects.requireNonNull(webSocketClient);
        this.webSocketClient.setPriceUpdateListener(this::notifyPriceUpdate);
    }

    @Override
    public synchronized BigDecimal getCurrentPrice(String ticker) throws IOException {
        String normalized = StockSymbol.normalize(ticker);
        webSocketClient.subscribe(normalized);

        BigDecimal cachedPrice = webSocketClient.getLatestPrice(normalized);
        if (cachedPrice != null) {
            return cachedPrice;
        }

        Request request = new Request.Builder()
                .url(SNAPSHOT_URL + "?symbols=" + normalized + "&feed=iex")
                .header("Accept", "application/json")
                .header("APCA-API-KEY-ID", apiKey)
                .header("APCA-API-SECRET-KEY", apiSecret)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() == 401 || response.code() == 403) {
                throw new IOException("Alpaca credentials were rejected.");
            }
            if (!response.isSuccessful()) {
                throw new IOException("Alpaca returned HTTP " + response.code() + ".");
            }
            if (response.body() == null) {
                throw new IOException("Alpaca returned an empty snapshot.");
            }

            AlpacaPriceParser.AlpacaSnapshot snapshot =
                    AlpacaPriceParser.parseSnapshot(response.body().string(), normalized);
            webSocketClient.setLatestPrice(normalized,
                    snapshot.getPrice(), snapshot.getTimestamp());
            return webSocketClient.getLatestPrice(normalized);
        } catch (IOException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IOException("Could not retrieve the Alpaca stock snapshot.");
        }
    }

    @Override
    public void close() {
        webSocketClient.close();
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }

    public void setPriceUpdateListener(
            BiConsumer<String, BigDecimal> priceUpdateListener) {
        this.priceUpdateListener = Objects.requireNonNull(
                priceUpdateListener, "priceUpdateListener must not be null");
    }

    private void notifyPriceUpdate(String ticker, BigDecimal price) {
        priceUpdateListener.accept(ticker, price);
    }

    private static String requireCredential(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Set " + name + " and APCA_API_SECRET_KEY in your run configuration.");
        }
        return value.trim();
    }
}
