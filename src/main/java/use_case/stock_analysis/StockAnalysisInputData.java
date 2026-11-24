package use_case.stock_analysis;

/**
 * Input data for the stock analysis use case.
 */
public class StockAnalysisInputData {

    private final String symbol;

    public StockAnalysisInputData(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
