package interface_adapter.stock_analysis;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Stores the state of the stock analysis view.
 */
public class StockAnalysisState {

    private final String symbol;
    private final NavigableMap<LocalDate, BigDecimal> closingPrices;
    private final String errorMessage;

    public StockAnalysisState() {
        this("", new TreeMap<>(), "");
    }

    public StockAnalysisState(
            String symbol,
            NavigableMap<LocalDate, BigDecimal> closingPrices,
            String errorMessage) {

        this.symbol = Objects.requireNonNull(symbol);
        this.closingPrices = Collections.unmodifiableNavigableMap(
                new TreeMap<>(closingPrices));
        this.errorMessage = Objects.requireNonNull(errorMessage);
    }

    public String getSymbol() {
        return symbol;
    }

    public NavigableMap<LocalDate, BigDecimal> getClosingPrices() {
        return closingPrices;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
