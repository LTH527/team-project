package use_case.stock_analysis;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.NavigableMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class StockAnalysisInteractorTest {

    private static class FakeDataAccess
            implements StockAnalysisDataAccessInterface {

        NavigableMap<LocalDate, BigDecimal> prices =
                new TreeMap<>();

        boolean shouldFail = false;
        String requestedSymbol;

        @Override
        public NavigableMap<LocalDate, BigDecimal>
        getDailyClosingPrices(String symbol)
                throws IOException {

            requestedSymbol = symbol;

            if (shouldFail) {
                throw new IOException("API error");
            }

            return prices;
        }
    }

    private static class FakePresenter
            implements StockAnalysisOutputBoundary {

        StockAnalysisOutputData outputData;
        String errorMessage;

        @Override
        public void prepareSuccessView(
                StockAnalysisOutputData outputData) {
            this.outputData = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    @Test
    void successTest() {
        FakeDataAccess dao = new FakeDataAccess();
        dao.prices.put(
                LocalDate.of(2026, 11, 20),
                new BigDecimal("250.50"));

        FakePresenter presenter = new FakePresenter();

        StockAnalysisInteractor interactor =
                new StockAnalysisInteractor(dao, presenter);

        interactor.execute(
                new StockAnalysisInputData("AAPL"));

        assertNotNull(presenter.outputData);
        assertNull(presenter.errorMessage);
        assertEquals("AAPL",
                presenter.outputData.getSymbol());
        assertEquals(1,
                presenter.outputData.getClosingPrices().size());
    }

    @Test
    void lowercaseSymbolTest() {
        FakeDataAccess dao = new FakeDataAccess();
        dao.prices.put(
                LocalDate.of(2026, 11, 20),
                new BigDecimal("250.50"));

        FakePresenter presenter = new FakePresenter();

        StockAnalysisInteractor interactor =
                new StockAnalysisInteractor(dao, presenter);

        interactor.execute(
                new StockAnalysisInputData("aapl"));

        assertEquals("AAPL", dao.requestedSymbol);
        assertNotNull(presenter.outputData);
    }

    @Test
    void emptyInputTest() {
        FakeDataAccess dao = new FakeDataAccess();
        FakePresenter presenter = new FakePresenter();

        StockAnalysisInteractor interactor =
                new StockAnalysisInteractor(dao, presenter);

        interactor.execute(
                new StockAnalysisInputData(""));

        assertNotNull(presenter.errorMessage);
        assertNull(presenter.outputData);
        assertNull(dao.requestedSymbol);
    }

    @Test
    void invalidSymbolTest() {
        FakeDataAccess dao = new FakeDataAccess();
        FakePresenter presenter = new FakePresenter();

        StockAnalysisInteractor interactor =
                new StockAnalysisInteractor(dao, presenter);

        interactor.execute(
                new StockAnalysisInputData("AAPL!"));

        assertNotNull(presenter.errorMessage);
        assertNull(presenter.outputData);
        assertNull(dao.requestedSymbol);
    }

    @Test
    void noDataTest() {
        FakeDataAccess dao = new FakeDataAccess();
        FakePresenter presenter = new FakePresenter();

        StockAnalysisInteractor interactor =
                new StockAnalysisInteractor(dao, presenter);

        interactor.execute(
                new StockAnalysisInputData("AAPL"));

        assertNotNull(presenter.errorMessage);
        assertNull(presenter.outputData);
    }

    @Test
    void apiFailureTest() {
        FakeDataAccess dao = new FakeDataAccess();
        dao.shouldFail = true;

        FakePresenter presenter = new FakePresenter();

        StockAnalysisInteractor interactor =
                new StockAnalysisInteractor(dao, presenter);

        interactor.execute(
                new StockAnalysisInputData("AAPL"));

        assertNotNull(presenter.errorMessage);
        assertNull(presenter.outputData);
    }
}
