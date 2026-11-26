package interface_adapter.stock_analysis;

import use_case.stock_analysis.StockSearchOutputBoundary;
import use_case.stock_analysis.StockSearchOutputData;

import java.util.List;
import java.util.Objects;

/**
 * Presenter for stock search results.
 */
public class StockSearchPresenter
        implements StockSearchOutputBoundary {

    private final StockSearchViewModel viewModel;

    public StockSearchPresenter(
            StockSearchViewModel viewModel) {
        this.viewModel = Objects.requireNonNull(viewModel);
    }

    @Override
    public void prepareSuccessView(
            StockSearchOutputData outputData) {

        StockSearchState state = new StockSearchState(
                outputData.getResults(),
                ""
        );

        viewModel.setState(state);
    }

    @Override
    public void prepareFailView(String errorMessage) {

        StockSearchState state = new StockSearchState(
                List.of(),
                errorMessage
        );

        viewModel.setState(state);
    }
}
