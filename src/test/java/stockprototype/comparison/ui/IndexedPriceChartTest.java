package stockprototype.comparison.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import stockprototype.StockDataSource;
import stockprototype.StockTrade;
import stockprototype.comparison.ComparisonInteractor;
import stockprototype.comparison.ComparisonResult;

class IndexedPriceChartTest {
    private static final LocalDate FRIDAY = LocalDate.of(2026, 11, 20);
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 23);
    private static final String LIVE_LABEL = "Nov 23, 2026 · Live";
    private static final StockTrade CURRENT = new StockTrade(new BigDecimal("145.25"),
            Instant.parse("2026-11-23T17:00:00Z"));
    // This UTC date is already Monday, but the market date in New York is still Sunday.
    private static final StockTrade STALE = new StockTrade(new BigDecimal("125.50"),
            Instant.parse("2026-11-23T00:30:00Z"));

    @Test
    void bothCurrentTradesGetALiveLabelWithoutChangingCompletedCloses() throws IOException {
        ComparisonResult result = compare(FRIDAY, Map.of("NVDA", CURRENT, "TSLA", CURRENT));
        String description = describe(result, true);
        assertTrue(description.contains(LIVE_LABEL), description);
        String liveSymbols = description.substring(description.indexOf(LIVE_LABEL));
        assertTrue(liveSymbols.contains("NVDA") && liveSymbols.contains("TSLA"), liveSymbols);
        assertEquals(2, result.days.size());
        assertEquals(FRIDAY, result.days.get(1).date);
        assertEquals(0, result.firstChange.compareTo(BigDecimal.TEN));
    }

    @Test
    void onlyTheSymbolWithAMarketDateTradeGetsALivePoint() throws IOException {
        for (String freshSymbol : new String[]{"NVDA", "TSLA"}) {
            String staleSymbol = freshSymbol.equals("NVDA") ? "TSLA" : "NVDA";
            ComparisonResult result = compare(FRIDAY, Map.of(freshSymbol, CURRENT, staleSymbol, STALE));
            String description = describe(result, true);
            assertTrue(description.contains(LIVE_LABEL), description);
            String liveSymbols = description.substring(description.indexOf(LIVE_LABEL));
            assertTrue(liveSymbols.contains(freshSymbol), liveSymbols);
            assertFalse(liveSymbols.contains(staleSymbol), liveSymbols);
        }
    }

    @Test
    void omitsLiveLabelForStaleOrMissingTradesAndWhenHistoryAlreadyIncludesToday() throws IOException {
        assertFalse(describe(compare(FRIDAY, Map.of("NVDA", STALE)), true).contains(LIVE_LABEL));
        assertFalse(describe(compare(FRIDAY, Map.of()), true).contains(LIVE_LABEL));
        assertFalse(describe(compare(MONDAY, Map.of("NVDA", CURRENT)), true).contains(LIVE_LABEL));
    }

    @Test
    void syntheticTradesAreNeverLabelledLive() throws IOException {
        assertFalse(describe(compare(FRIDAY, Map.of("TSLA", CURRENT)), false).contains(LIVE_LABEL));
    }

    private static String describe(ComparisonResult result, boolean live) {
        IndexedPriceChart chart = new IndexedPriceChart();
        chart.setResult(result, live, MONDAY);
        return chart.getAccessibleContext().getAccessibleDescription();
    }

    private static ComparisonResult compare(LocalDate lastClose, Map<String, StockTrade> trades) throws IOException {
        StockDataSource source = new StockDataSource() {
            @Override
            public NavigableMap<LocalDate, BigDecimal> getDailyClosingPrices(String symbol) {
                return new TreeMap<>(Map.of(FRIDAY.minusDays(1), new BigDecimal("100"),
                        lastClose, new BigDecimal("110")));
            }

            @Override
            public Optional<StockTrade> getLatestTrade(String symbol) {
                return Optional.ofNullable(trades.get(symbol));
            }
        };
        return new ComparisonInteractor(source).compare("NVDA", "TSLA", 20);
    }
}
