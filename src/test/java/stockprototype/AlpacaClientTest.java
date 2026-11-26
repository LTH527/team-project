package stockprototype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class AlpacaClientTest {
    @Test
    void listsActiveUsExchangeSymbolsWithoutRequiringBrokerageTradability() throws IOException {
        JSONArray assets = new JSONArray()
                .put(asset("TSLA", "NASDAQ", "active", "us_equity"))
                .put(asset("BRK.B", "NYSE", "active", "us_equity"))
                .put(asset("nvda", "NASDAQ", "active", "us_equity"))
                .put(asset("NVDA", "NASDAQ", "active", "us_equity"))
                .put(asset("OLD", "NYSE", "inactive", "us_equity"))
                .put(asset("OTCSTOCK", "OTC", "active", "us_equity"))
                .put(asset("BTCUSD", "CRYPTO", "active", "crypto"))
                .put(asset("BAD/SYMBOL", "NYSE", "active", "us_equity"));
        for (String id : List.of("PK-test", "AK-test")) {
            String host = id.startsWith("PK") ? "paper-api.alpaca.markets" : "api.alpaca.markets";
            AlpacaClient client = new AlpacaClient(id, "sample-secret", request -> {
                assertEquals("GET", request.method());
                assertEquals("https://" + host + "/v2/assets?status=active&asset_class=us_equity",
                        request.uri().toString());
                assertEquals(Optional.of(id), request.headers().firstValue("APCA-API-KEY-ID"));
                return new AlpacaClient.Reply(200, assets.toString());
            });
            assertEquals(List.of("BRK.B", "NVDA", "TSLA"), client.getSymbols());
        }
    }

    private static JSONObject asset(String symbol, String exchange, String status, String assetClass) {
        return new JSONObject().put("symbol", symbol).put("exchange", exchange).put("status", status)
                .put("class", assetClass).put("tradable", false);
    }

    @Test
    void returnsTickerAndDisplayNameTogetherFromOneDirectoryRequest() throws IOException {
        JSONArray assets = new JSONArray()
                .put(asset("NVDA", "NASDAQ", "active", "us_equity"))
                .put(asset("NVDA", "NASDAQ", "active", "us_equity").put("name", "NVIDIA Corporation"))
                .put(asset("AAPL", "NASDAQ", "active", "us_equity").put("name", "Apple Inc."))
                .put(asset("BRK.B", "NYSE", "active", "us_equity"));
        AtomicInteger requests = new AtomicInteger();
        AlpacaClient client = new AlpacaClient("PK-test", "sample-secret", request -> {
            requests.incrementAndGet();
            return new AlpacaClient.Reply(200, assets.toString());
        });
        List<StockListing> listings = client.loadListings(ignored -> fail("No cached provider response expected"));
        assertEquals(1, requests.get());
        assertEquals(3, listings.size());
        assertEquals("AAPL", listings.get(0).symbol);
        assertEquals("Apple Inc.", listings.get(0).name);
        assertEquals("", listings.get(1).name);
        assertEquals("NVIDIA Corporation", listings.get(2).name);
    }

    @Test
    void reportsDirectoryErrorsWithoutReturningProviderDetails() {
        for (AlpacaClient client : List.of(replying(200, "{}"), replying(200, "[{}]"),
                replying(429, "private provider details"))) {
            IOException error = assertThrows(IOException.class, client::getSymbols);
            assertFalse(error.getMessage().contains("private provider details"));
        }
    }

    private static JSONObject bar(String time, String close) {
        return new JSONObject().put("t", time).put("c", new BigDecimal(close));
    }

    private static String bars(String symbol, JSONArray values, Object next) {
        return new JSONObject().put("bars", new JSONObject().put(symbol, values))
                .put("next_page_token", next).toString();
    }

    private static AlpacaClient replying(int code, String body) {
        return new AlpacaClient("sample-id", "sample-secret", request -> new AlpacaClient.Reply(code, body));
    }

    @Test
    void requestsRawHistoricalIexBarsAndFollowsTheEncodedPageToken() throws IOException {
        List<HttpRequest> requests = new ArrayList<>();
        ZoneId market = ZoneId.of("America/New_York");
        LocalDate today = LocalDate.now(market);
        JSONArray later = new JSONArray().put(bar("2024-11-13T05:00:00Z", "88.2501"))
                .put(bar(today.atStartOfDay(market).toInstant().toString(), "120"));
        JSONArray earlier = new JSONArray().put(bar("2024-07-11T04:00:00Z", "85.69"));
        AlpacaClient client = new AlpacaClient("sample-id", "sample-secret", request -> {
            requests.add(request);
            String body = requests.size() == 1 ? bars("IBM", later, "a+/=")
                    : bars("IBM", earlier, JSONObject.NULL);
            return new AlpacaClient.Reply(200, body);
        });

        NavigableMap<LocalDate, BigDecimal> values = client.getDailyClosingPrices(" ibm ");
        assertEquals(List.of(LocalDate.parse("2024-07-11"), LocalDate.parse("2024-11-13")),
                new ArrayList<>(values.keySet()));
        assertEquals(new BigDecimal("88.2501"), values.lastEntry().getValue());
        assertThrows(UnsupportedOperationException.class, values::clear);
        assertEquals(2, requests.size());
        HttpRequest first = requests.get(0);
        assertEquals("https", first.uri().getScheme());
        assertEquals("data.alpaca.markets", first.uri().getHost());
        assertEquals("/v2/stocks/bars", first.uri().getPath());
        String query = first.uri().getQuery();
        for (String param : List.of("symbols=IBM", "timeframe=1Day", "feed=iex", "adjustment=raw",
                "start=2016-01-01", "end=" + today.atStartOfDay(market).toInstant().minusSeconds(1))) {
            assertTrue(query.contains(param), param);
        }
        assertTrue(requests.get(1).uri().getRawQuery().contains("page_token=a%2B%2F%3D"));
        for (HttpRequest request : requests) {
            assertEquals(Optional.of("sample-id"), request.headers().firstValue("APCA-API-KEY-ID"));
            assertEquals(Optional.of("sample-secret"), request.headers().firstValue("APCA-API-SECRET-KEY"));
            assertFalse(request.uri().toString().contains("sample-"));
        }
    }

    @Test
    void readsLastTradeWithItsOriginalTimestamp() throws IOException {
        String time = "2024-11-13T15:01:02.123456789Z";
        JSONObject data = new JSONObject().put("t", time).put("p", new BigDecimal("235.6789"));
        String body = new JSONObject().put("trades", new JSONObject().put("AMD", data)).toString();
        AlpacaClient client = new AlpacaClient("sample-id", "sample-secret", request -> {
            assertEquals("/v2/stocks/trades/latest", request.uri().getPath());
            assertEquals("symbols=AMD&feed=iex", request.uri().getQuery());
            return new AlpacaClient.Reply(200, body);
        });
        StockTrade result = client.getLatestTrade("amd").orElseThrow();
        assertEquals(new BigDecimal("235.6789"), result.price);
        assertEquals(Instant.parse(time), result.time);
        assertTrue(replying(200, body).getLatestTrade("MSFT").isEmpty());
        assertThrows(IOException.class, () -> replying(200, "{}").getLatestTrade("AMD"));
    }

    @Test
    void rejectsMissingCredentialsAndInvalidSymbols() {
        for (String absent : new String[] {null, " ", "two\nlines"}) {
            assertThrows(IllegalArgumentException.class, () -> replyingWithCredentials(absent, "sample-secret"));
            assertThrows(IllegalArgumentException.class, () -> replyingWithCredentials("sample-id", absent));
        }
        AlpacaClient client = replyingWithCredentials("sample-id", "sample-secret");
        assertThrows(IllegalArgumentException.class, () -> client.getDailyClosingPrices("IBM&feed=sip"));
        assertThrows(IllegalArgumentException.class, () -> client.getLatestTrade("NVDA/TSLA"));
    }

    private static AlpacaClient replyingWithCredentials(String id, String secret) {
        return new AlpacaClient(id, secret, request -> fail("No request expected"));
    }

    @Test
    void rejectsMalformedMissingAndNonpositivePrices() {
        JSONArray zero = new JSONArray().put(bar("2024-11-13T05:00:00Z", "0"));
        JSONArray badDate = new JSONArray().put(bar("not-a-date", "125"));
        for (String body : List.of("junk", "{}", "{\"bars\":{}}", bars("AAPL", zero, JSONObject.NULL),
                bars("IBM", zero, JSONObject.NULL), bars("IBM", badDate, JSONObject.NULL))) {
            assertThrows(IOException.class, () -> replying(200, body).getDailyClosingPrices("IBM"));
        }
    }

    @Test
    void stopsOnRepeatedPageToken() {
        String body = bars("IBM", new JSONArray(), "stuck");
        assertThrows(IOException.class, () -> replying(200, body).getDailyClosingPrices("IBM"));
    }

    @Test
    void reportsHttpErrorsWithoutLeakingProviderDetails() {
        for (int status : new int[] {301, 401, 403, 429, 500}) {
            IOException error = assertThrows(IOException.class,
                    () -> replying(status, "private provider response").getDailyClosingPrices("IBM"));
            assertFalse(error.getMessage().contains("private provider response"));
        }
    }

    @Test
    void reportsTransportFailureAndPreservesInterruption() {
        AlpacaClient offline = new AlpacaClient("sample-id", "sample-secret", request -> {
            throw new IOException("private transport details");
        });
        IOException failure = assertThrows(IOException.class, () -> offline.getLatestTrade("IBM"));
        assertFalse(failure.getMessage().contains("private transport details"));
        assertNull(failure.getCause());

        AlpacaClient interrupted = new AlpacaClient("sample-id", "sample-secret", request -> {
            throw new InterruptedException();
        });
        boolean interruptRestored;
        try {
            assertThrows(IOException.class, () -> interrupted.getDailyClosingPrices("IBM"));
        } finally {
            interruptRestored = Thread.interrupted();
        }
        assertTrue(interruptRestored, "The client should restore the interrupt flag");
    }
}
