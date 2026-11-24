package interface_adapter.stock_analysis;

import use_case.stock_analysis.StockAnalysisOutputBoundary;
import use_case.stock_analysis.StockAnalysisOutputData;

import java.util.Objects;
import java.util.TreeMap;

/**
 * Presenter for the stock analysis use case.
 */
public class StockAnalysisPresenter
        implements StockAnalysisOutputBoundary {

    private final StockAnalysisViewModel viewModel;

    public StockAnalysisPresenter(
            StockAnalysisViewModel viewModel) {
        this.viewModel = Objects.requireNonNull(viewModel);
    }

    @Override
    public void prepareSuccessView(
            StockAnalysisOutputData outputData) {

        StockAnalysisState state = new StockAnalysisState(
                outputData.getSymbol(),
                outputData.getClosingPrices(),
                ""
        );

        viewModel.setState(state);
    }

    @Override
    public void prepareFailView(String errorMessage) {

        StockAnalysisState state = new StockAnalysisState(
                "",
                new TreeMap<>(),
                errorMessage
        );

        viewModel.setState(state);
    }
}
