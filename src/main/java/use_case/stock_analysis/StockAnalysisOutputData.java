package use_case.stock_analysis;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Output data for a single stock analysis.
 */
public class StockAnalysisOutputData {

    private final String symbol;
    private final NavigableMap<LocalDate, BigDecimal> closingPrices;

    public StockAnalysisOutputData(
            String symbol,
            NavigableMap<LocalDate, BigDecimal> closingPrices) {
        this.symbol = symbol;
        this.closingPrices = Collections.unmodifiableNavigableMap(
                new TreeMap<>(closingPrices));
    }

    public String getSymbol() {
        return symbol;
    }

    public NavigableMap<LocalDate, BigDecimal> getClosingPrices() {
        return closingPrices;
    }
}
