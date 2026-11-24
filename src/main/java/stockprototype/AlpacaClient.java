package stockprototype;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Reads raw daily closes and the latest eligible IEX trade from Alpaca Market Data. */
public final class AlpacaClient implements StockDataSource {
    private static final String STOCKS_URL = "https://data.alpaca.markets/v2/stocks/";
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final String HISTORY_START = "2016-01-01T05:00:00Z";
    private final String keyId;
    private final String secret;
    private final Transport transport;

    public static AlpacaClient fromEnvironment() {
        return new AlpacaClient(System.getenv("APCA_API_KEY_ID"), System.getenv("APCA_API_SECRET_KEY"));
    }

    public AlpacaClient(String keyId, String secret) {
        this(keyId, secret, new HttpTransport());
    }

    AlpacaClient(String keyId, String secret, Transport transport) {
        this.keyId = credential(keyId, "APCA_API_KEY_ID");
        this.secret = credential(secret, "APCA_API_SECRET_KEY");
        this.transport = transport;
    }

    private static String credential(String value, String name) {
        if (value == null || !value.trim().matches("[!-~]+")) {
            throw new IllegalArgumentException("Set " + name + " in your local run configuration.");
        }
        return value.trim();
    }

    @Override
    public List<String> getSymbols() throws IOException {
        return getListings().stream().map(listing -> listing.symbol).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<StockListing> getListings() throws IOException {
        String host = keyId.startsWith("AK") ? "api.alpaca.markets" : "paper-api.alpaca.markets";
        URI uri = URI.create("https://" + host + "/v2/assets?status=active&asset_class=us_equity");
        String body = requestBody(uri);
        NavigableMap<String, StockListing> listings = new TreeMap<>();
        try {
            JSONArray assets = new JSONArray(body);
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                if ("active".equals(asset.getString("status")) && "us_equity".equals(asset.getString("class"))
                        && !"OTC".equals(asset.getString("exchange"))) {
                    String symbol = asset.getString("symbol").toUpperCase(Locale.ROOT);
                    if (StockSymbol.isSupported(symbol)) {
                        StockListing listing = new StockListing(symbol, asset.optString("name", ""));
                        listings.merge(symbol, listing, (first, next) -> first.name.isEmpty() ? next : first);
                    }
                }
            }
        } catch (JSONException exception) {
            throw new IOException("Alpaca returned an unreadable stock directory.");
        }
        return List.copyOf(listings.values());
    }

    @Override
    public NavigableMap<LocalDate, BigDecimal> getDailyClosingPrices(String symbol) throws IOException {
        String ticker = StockSymbol.normalize(symbol);
        LocalDate today = LocalDate.now(NEW_YORK);
        // Alpaca includes both endpoints. Exclude today's still-changing daily bar.
        Instant end = today.atStartOfDay(NEW_YORK).toInstant().minusSeconds(1);
        String query = "bars?symbols=" + ticker + "&timeframe=1Day&feed=iex&adjustment=raw"
                + "&start=" + HISTORY_START + "&end=" + end + "&sort=asc&limit=10000";
        NavigableMap<LocalDate, BigDecimal> prices = new TreeMap<>();
        Set<String> seenTokens = new HashSet<>();
        String token = null;
        do {
            String pageQuery = token == null ? query
                    : query + "&page_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
            JSONObject page = request(pageQuery);
            try {
                JSONObject bars = page.getJSONObject("bars");
                if (bars.has(ticker)) {
                    JSONArray rows = bars.getJSONArray(ticker);
                    for (int i = 0; i < rows.length(); i++) {
                        JSONObject bar = rows.getJSONObject(i);
                        LocalDate date = timestamp(bar).atZone(NEW_YORK).toLocalDate();
                        if (date.isBefore(today)) {
                            prices.put(date, price(bar, "c"));
                        }
                    }
                }
                token = page.isNull("next_page_token") ? null : page.getString("next_page_token");
            } catch (JSONException | DateTimeParseException exception) {
                throw new IOException("Alpaca returned invalid daily price data.");
            }
            if (token != null && (token.isEmpty() || !seenTokens.add(token))) {
                throw new IOException("Alpaca returned an invalid page token.");
            }
        } while (token != null);
        if (prices.isEmpty()) {
            throw new IOException("No IEX daily prices for " + ticker + ". Check the stock symbol.");
        }
        return Collections.unmodifiableNavigableMap(prices);
    }

    @Override
    public Optional<StockTrade> getLatestTrade(String symbol) throws IOException {
        String ticker = StockSymbol.normalize(symbol);
        return parseLatestTrade(request("trades/latest?symbols=" + ticker + "&feed=iex"), ticker);
    }

    static Optional<StockTrade> parseLatestTrade(JSONObject page, String ticker) throws IOException {
        try {
            JSONObject trades = page.getJSONObject("trades");
            if (trades.isNull(ticker)) {
                return Optional.empty();
            }
            JSONObject trade = trades.getJSONObject(ticker);
            return Optional.of(new StockTrade(price(trade, "p"), timestamp(trade)));
        } catch (JSONException | DateTimeParseException exception) {
            throw new IOException("Alpaca returned invalid latest trade data.");
        }
    }

    private static Instant timestamp(JSONObject row) {
        return Instant.parse(row.getString("t"));
    }

    private static BigDecimal price(JSONObject row, String field) throws IOException {
        BigDecimal amount = row.getBigDecimal(field);
        if (amount.signum() <= 0) {
            throw new IOException("Alpaca returned a nonpositive price.");
        }
        return amount;
    }

    private JSONObject request(String query) throws IOException {
        String body = requestBody(URI.create(STOCKS_URL + query));
        try {
            return new JSONObject(body);
        } catch (JSONException exception) {
            throw new IOException("Alpaca returned an unreadable response.");
        }
    }

    private String requestBody(URI uri) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("APCA-API-KEY-ID", keyId)
                .header("APCA-API-SECRET-KEY", secret)
                .header("Accept", "application/json")
                .GET().build();
        Reply reply;
        try {
            reply = transport.get(request);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("The Alpaca request was interrupted.");
        } catch (IOException exception) {
            throw new IOException("Could not reach Alpaca. Check your connection and try again.");
        }
        switch (reply.status) {
            case 200:
                return reply.body;
            case 401:
                throw new IOException("Alpaca rejected the credentials. Check your key ID and secret.");
            case 403:
                throw new IOException("Alpaca denied access. Check your account's API access.");
            case 429:
                throw new IOException("Alpaca's request limit was reached. Wait a minute and try again.");
            default:
                throw new IOException("Alpaca returned HTTP " + reply.status + ".");
        }
    }

    interface Transport {
        Reply get(HttpRequest request) throws IOException, InterruptedException;
    }

    static final class Reply {
        private final int status;
        private final String body;

        Reply(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }

    private static final class HttpTransport implements Transport {
        private final HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(10)).build();

        @Override
        public Reply get(HttpRequest request) throws IOException, InterruptedException {
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Reply(response.statusCode(), response.body());
        }
    }
}
