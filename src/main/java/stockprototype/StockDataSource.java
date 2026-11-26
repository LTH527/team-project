package stockprototype;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;

/** Price history used by the comparison, independent of the underlying provider. */
public interface StockDataSource extends StockSymbolDirectory {
    /** Raw daily closing prices, sorted oldest first. No currency conversion or adjustment. */
    NavigableMap<LocalDate, BigDecimal> getDailyClosingPrices(String symbol) throws IOException;

    /** Sources that only supply prices can still be used for manual ticker entry. */
    @Override
    default List<String> getSymbols() throws IOException {
        return List.of();
    }

    /** Most recent individual trade when the source has one. */
    default Optional<StockTrade> getLatestTrade(String symbol) throws IOException {
        return Optional.empty();
    }
}
