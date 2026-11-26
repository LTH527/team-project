package interface_adapter.stock_analysis;

import entity.StockSearchResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Stores the current stock search results and error message.
 */
public class StockSearchState {

    private final List<StockSearchResult> results;
    private final String errorMessage;

    public StockSearchState() {
        this(List.of(), "");
    }

    public StockSearchState(
            List<StockSearchResult> results,
            String errorMessage) {

        this.results = Collections.unmodifiableList(
                new ArrayList<>(Objects.requireNonNull(results)));
        this.errorMessage = Objects.requireNonNull(errorMessage);
    }

    public List<StockSearchResult> getResults() {
        return results;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
