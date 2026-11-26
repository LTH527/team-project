package use_case.stock_analysis;

import entity.StockSearchResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Output data containing matching stocks.
 */
public class StockSearchOutputData {

    private final List<StockSearchResult> results;

    public StockSearchOutputData(
            List<StockSearchResult> results) {
        this.results = Collections.unmodifiableList(
                new ArrayList<>(results));
    }

    public List<StockSearchResult> getResults() {
        return results;
    }
}
