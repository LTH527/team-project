package interface_adapter.stock_analysis;

import org.junit.jupiter.api.Test;
import use_case.stock_analysis.StockAnalysisOutputData;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.NavigableMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class StockAnalysisPresenterTest {

    @Test
    void successTest() {
        StockAnalysisViewModel viewModel =
                new StockAnalysisViewModel();

        StockAnalysisPresenter presenter =
                new StockAnalysisPresenter(viewModel);

        NavigableMap<LocalDate, BigDecimal> prices =
                new TreeMap<>();

        prices.put(
                LocalDate.of(2026, 11, 20),
                new BigDecimal("250.50")
        );

        StockAnalysisOutputData outputData =
                new StockAnalysisOutputData("AAPL", prices);

        presenter.prepareSuccessView(outputData);

        StockAnalysisState state = viewModel.getState();

        assertEquals("AAPL", state.getSymbol());
        assertEquals(1, state.getClosingPrices().size());
        assertEquals("", state.getErrorMessage());
    }

    @Test
    void failureTest() {
        StockAnalysisViewModel viewModel =
                new StockAnalysisViewModel();

        StockAnalysisPresenter presenter =
                new StockAnalysisPresenter(viewModel);

        presenter.prepareFailView("Invalid stock symbol.");

        StockAnalysisState state = viewModel.getState();

        assertEquals("", state.getSymbol());
        assertTrue(state.getClosingPrices().isEmpty());
        assertEquals(
                "Invalid stock symbol.",
                state.getErrorMessage()
        );
    }

    @Test
    void stateChangeNotificationTest() {
        StockAnalysisViewModel viewModel =
                new StockAnalysisViewModel();

        StockAnalysisPresenter presenter =
                new StockAnalysisPresenter(viewModel);

        final int[] notifications = {0};

        viewModel.addPropertyChangeListener(event -> {
            if (StockAnalysisViewModel.STATE_PROPERTY
                    .equals(event.getPropertyName())) {
                notifications[0]++;
            }
        });

        presenter.prepareFailView("API error");

        assertEquals(1, notifications[0]);
    }
}
