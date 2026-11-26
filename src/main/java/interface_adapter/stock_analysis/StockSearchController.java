package interface_adapter.stock_analysis;

import use_case.stock_analysis.StockSearchInputBoundary;
import use_case.stock_analysis.StockSearchInputData;

import java.util.Objects;

/**
 * Controller for searching stocks by name or symbol.
 */
public class StockSearchController {

    private final StockSearchInputBoundary interactor;

    public StockSearchController(
            StockSearchInputBoundary interactor) {
        this.interactor = Objects.requireNonNull(interactor);
    }

    /**
     * Executes a stock search for the given keywords.
     *
     * @param keywords company name or stock symbol
     */
    public void execute(String keywords) {
        StockSearchInputData inputData =
                new StockSearchInputData(keywords);

        interactor.execute(inputData);
    }
}
