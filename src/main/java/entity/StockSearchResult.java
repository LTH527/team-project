package entity;

import java.util.Objects;

/**
 * Represents a stock returned by a company search.
 */
public class StockSearchResult {

    private final String symbol;
    private final String companyName;

    public StockSearchResult(String symbol, String companyName) {
        this.symbol = Objects.requireNonNull(symbol);
        this.companyName = Objects.requireNonNull(companyName);
    }

    public String getSymbol() {
        return symbol;
    }

    public String getCompanyName() {
        return companyName;
    }
}
