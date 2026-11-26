package stockprototype;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.json.JSONException;
import org.json.JSONObject;

/** Reads the bundled synthetic price histories for offline comparisons. */
final class SampleStockDataSource implements StockDataSource {
    private static final Map<String, String> NAMES = Map.of(
            "AAPL", "Apple Inc.",
            "IBM", "International Business Machines Corporation",
            "MSFT", "Microsoft Corporation",
            "NVDA", "NVIDIA Corporation",
            "TSLA", "Tesla, Inc.");

    @Override
    public List<StockListing> getListings() throws IOException {
        return getSymbols().stream().map(symbol -> new StockListing(symbol, NAMES.get(symbol)))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<String> getSymbols() throws IOException {
        try (BufferedReader reader = openHistory()) {
            List<String> columns = readColumns(reader);
            return List.copyOf(new TreeSet<>(columns.subList(1, columns.size())));
        }
    }

    private BufferedReader openHistory() throws IOException {
        InputStream stream = getClass().getResourceAsStream("/comparison-sample.csv");
        if (stream == null) {
            throw new IOException("The sample price file is missing.");
        }
        return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    private static List<String> readColumns(BufferedReader reader) throws IOException {
        String header = reader.readLine();
        if (header == null) {
            throw new IOException("The sample price file is empty.");
        }
        return Arrays.asList(header.split(","));
    }

    @Override
    public NavigableMap<LocalDate, BigDecimal> getDailyClosingPrices(String symbol) throws IOException {
        String ticker = StockSymbol.normalize(symbol);
        try (BufferedReader reader = openHistory()) {
            List<String> columns = readColumns(reader);
            int column = columns.indexOf(ticker);
            if (column < 1) {
                String symbols = String.join(", ", columns.subList(1, columns.size()));
                throw new IOException("Available sample symbols: " + symbols + ".");
            }
            NavigableMap<LocalDate, BigDecimal> history = new TreeMap<>();
            String row;
            while ((row = reader.readLine()) != null) {
                String[] fields = row.split(",");
                history.put(LocalDate.parse(fields[0]), new BigDecimal(fields[column]));
            }
            return Collections.unmodifiableNavigableMap(history);
        }
    }

    @Override
    public Optional<StockTrade> getLatestTrade(String symbol) throws IOException {
        String ticker = StockSymbol.normalize(symbol);
        try (InputStream stream = getClass().getResourceAsStream("/sample-trades.json")) {
            if (stream == null) {
                throw new IOException("The sample trades file is missing.");
            }
            JSONObject trades = new JSONObject(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            return AlpacaClient.parseLatestTrade(trades, ticker);
        } catch (JSONException exception) {
            throw new IOException("The sample trades file is malformed.", exception);
        }
    }
}
