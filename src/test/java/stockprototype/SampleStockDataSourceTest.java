package stockprototype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.NavigableMap;
import org.junit.jupiter.api.Test;

class SampleStockDataSourceTest {
    private final SampleStockDataSource source = new SampleStockDataSource();

    @Test
    void listsTheAvailableDemoSymbolsInAlphabeticalOrder() throws IOException {
        assertEquals(List.of("AAPL", "IBM", "MSFT", "NVDA", "TSLA"), source.getSymbols());
        List<StockListing> listings = source.getListings();
        assertEquals("AAPL", listings.get(0).symbol);
        assertEquals("Apple Inc.", listings.get(0).name);
        assertEquals("NVIDIA Corporation", listings.get(3).name);
        assertTrue(listings.stream().noneMatch(entry -> entry.name.isBlank()));
    }

    @Test
    void everyDemoSymbolHasHistoryAndASeparateSyntheticTrade() throws IOException {
        for (String symbol : List.of("IBM", "AAPL", "MSFT", "NVDA", "TSLA")) {
            NavigableMap<LocalDate, BigDecimal> history = source.getDailyClosingPrices(symbol);
            assertTrue(history.size() >= 60);
            assertEquals(LocalDate.of(2026, 11, 13), history.lastKey());
            StockTrade trade = source.getLatestTrade(symbol).orElseThrow();
            assertTrue(trade.price.signum() > 0);
            assertTrue(trade.time.isAfter(Instant.parse("2026-11-13T21:00:00Z")));
        }
        assertEquals(new BigDecimal("234.79"), source.getLatestTrade(" tsla ").orElseThrow().price);
    }

    @Test
    void namesTheAvailableDemoSymbolsForAnUnsupportedTicker() {
        IOException failure = assertThrows(IOException.class, () -> source.getDailyClosingPrices("UNKNOWN"));
        assertTrue(failure.getMessage().contains("IBM, AAPL, MSFT, NVDA, TSLA"));
    }
}
