package interface_adapter.stock_analysis;

import use_case.stock_analysis.StockAnalysisInputBoundary;
import use_case.stock_analysis.StockAnalysisInputData;

import java.util.Objects;

/**
 * Controller for the stock analysis use case.
 */
public class StockAnalysisController {

    private final StockAnalysisInputBoundary interactor;

    public StockAnalysisController(
            StockAnalysisInputBoundary interactor) {
        this.interactor = Objects.requireNonNull(interactor);
    }

    /**
     * Executes stock analysis for the given symbol.
     *
     * @param symbol the stock symbol entered by the user
     */
    public void execute(String symbol) {
        StockAnalysisInputData inputData =
                new StockAnalysisInputData(symbol);

        interactor.execute(inputData);
    }
}
