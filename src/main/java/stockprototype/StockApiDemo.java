package stockprototype;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.NavigableMap;
import java.util.Optional;

/** Prints one stock's prices or opens the two-stock comparison screen. */
public final class StockApiDemo {
    private StockApiDemo() { }

    public static void main(String[] args) {
        if (args.length > 0 && "--compare".equals(args[0])) {
            StockComparisonApp.main(Arrays.copyOfRange(args, 1, args.length));
            return;
        }
        try {
            StockDataSource prices;
            String heading;
            if (args.length == 0 || (args.length <= 2 && "--demo".equals(args[0]))) {
                prices = new SampleStockDataSource();
                heading = "OFFLINE DEMO (synthetic prices)";
            } else if (args.length == 2 && "--live".equals(args[0])) {
                prices = AlpacaClient.fromEnvironment();
                heading = "ALPACA IEX (market data)";
            } else {
                throw new IllegalArgumentException(
                        "Usage: StockApiDemo [--demo [SYMBOL] | --live SYMBOL | --compare [--demo | --live]]");
            }
            String symbol = args.length == 2 ? StockSymbol.normalize(args[1]) : "IBM";
            NavigableMap<LocalDate, BigDecimal> history = prices.getDailyClosingPrices(symbol);

            System.out.println(heading);
            System.out.println("Symbol: " + symbol);
            System.out.println("Daily closing prices: " + history.size());
            System.out.println("Earliest date: " + history.firstKey());
            System.out.println("Latest date: " + history.lastKey());
            history.entrySet().stream().skip(Math.max(0, history.size() - 5))
                    .forEach(entry -> System.out.println(entry.getKey() + "  " + entry.getValue().toPlainString()));
            printLatestTrade(prices, symbol);
        } catch (IOException | IllegalArgumentException exception) {
            System.err.println(exception.getMessage());
            System.exit(1);
        }
    }

    private static void printLatestTrade(StockDataSource prices, String symbol) {
        try {
            Optional<StockTrade> latest = prices.getLatestTrade(symbol);
            if (latest.isPresent()) {
                StockTrade trade = latest.get();
                System.out.println("Latest trade: " + trade.price.toPlainString() + " at " + trade.time);
            } else {
                System.out.println("Latest trade unavailable.");
            }
        } catch (IOException exception) {
            System.out.println("Latest trade unavailable. " + exception.getMessage());
        }
    }
}
