package interface_adapter.stock_analysis;

import entity.StockSearchResult;
import org.junit.jupiter.api.Test;
import use_case.stock_analysis.StockSearchOutputData;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StockSearchPresenterTest {

    @Test
    void successTest() {
        StockSearchViewModel viewModel =
                new StockSearchViewModel();

        StockSearchPresenter presenter =
                new StockSearchPresenter(viewModel);

        StockSearchOutputData outputData =
                new StockSearchOutputData(
                        List.of(new StockSearchResult(
                                "AAPL", "Apple Inc."))
                );

        presenter.prepareSuccessView(outputData);

        StockSearchState state = viewModel.getState();

        assertEquals(1, state.getResults().size());
        assertEquals("AAPL",
                state.getResults().get(0).getSymbol());
        assertEquals("Apple Inc.",
                state.getResults().get(0).getCompanyName());
        assertEquals("", state.getErrorMessage());
    }

    @Test
    void failureTest() {
        StockSearchViewModel viewModel =
                new StockSearchViewModel();

        StockSearchPresenter presenter =
                new StockSearchPresenter(viewModel);

        presenter.prepareFailView(
                "No matching stocks found.");

        StockSearchState state = viewModel.getState();

        assertTrue(state.getResults().isEmpty());
        assertEquals("No matching stocks found.",
                state.getErrorMessage());
    }

    @Test
    void stateChangeNotificationTest() {
        StockSearchViewModel viewModel =
                new StockSearchViewModel();

        StockSearchPresenter presenter =
                new StockSearchPresenter(viewModel);

        final int[] notifications = {0};

        viewModel.addPropertyChangeListener(event -> {
            if (StockSearchViewModel.STATE_PROPERTY
                    .equals(event.getPropertyName())) {
                notifications[0]++;
            }
        });

        presenter.prepareFailView("Search error");

        assertEquals(1, notifications[0]);
    }
}
