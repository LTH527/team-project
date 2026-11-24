package use_case.stock_analysis;

/**
 * Input data for searching stocks.
 */
public class StockSearchInputData {

    private final String keywords;

    public StockSearchInputData(String keywords) {
        this.keywords = keywords;
    }

    public String getKeywords() {
        return keywords;
    }
}
