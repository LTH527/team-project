package use_case.stock_analysis;

import entity.StockSearchResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StockSearchInteractorTest {

    private static class FakeDataAccess
            implements StockSearchDataAccessInterface {

        List<StockSearchResult> results = List.of();
        String receivedKeywords;
        boolean shouldFail = false;

        @Override
        public List<StockSearchResult> searchStocks(
                String keywords) throws IOException {

            receivedKeywords = keywords;

            if (shouldFail) {
                throw new IOException("API error");
            }

            return results;
        }
    }

    private static class FakePresenter
            implements StockSearchOutputBoundary {

        StockSearchOutputData outputData;
        String errorMessage;

        @Override
        public void prepareSuccessView(
                StockSearchOutputData outputData) {
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

        dao.results = List.of(
                new StockSearchResult("AAPL", "Apple Inc.")
        );

        FakePresenter presenter = new FakePresenter();

        StockSearchInteractor interactor =
                new StockSearchInteractor(dao, presenter);

        interactor.execute(
                new StockSearchInputData("Apple"));

        assertNotNull(presenter.outputData);
        assertNull(presenter.errorMessage);
        assertEquals(1,
                presenter.outputData.getResults().size());
        assertEquals("AAPL",
                presenter.outputData.getResults()
                        .get(0).getSymbol());
    }

    @Test
    void trimKeywordsTest() {
        FakeDataAccess dao = new FakeDataAccess();

        dao.results = List.of(
                new StockSearchResult("AAPL", "Apple Inc.")
        );

        FakePresenter presenter = new FakePresenter();

        StockSearchInteractor interactor =
                new StockSearchInteractor(dao, presenter);

        interactor.execute(
                new StockSearchInputData("  Apple  "));

        assertEquals("Apple", dao.receivedKeywords);
        assertNotNull(presenter.outputData);
    }

    @Test
    void emptyInputTest() {
        FakeDataAccess dao = new FakeDataAccess();
        FakePresenter presenter = new FakePresenter();

        StockSearchInteractor interactor =
                new StockSearchInteractor(dao, presenter);

        interactor.execute(
                new StockSearchInputData(""));

        assertNotNull(presenter.errorMessage);
        assertNull(presenter.outputData);
        assertNull(dao.receivedKeywords);
    }

    @Test
    void noMatchesTest() {
        FakeDataAccess dao = new FakeDataAccess();
        FakePresenter presenter = new FakePresenter();

        StockSearchInteractor interactor =
                new StockSearchInteractor(dao, presenter);

        interactor.execute(
                new StockSearchInputData("NOTREAL"));

        assertEquals("No matching stocks found.",
                presenter.errorMessage);
        assertNull(presenter.outputData);
    }

    @Test
    void apiFailureTest() {
        FakeDataAccess dao = new FakeDataAccess();
        dao.shouldFail = true;

        FakePresenter presenter = new FakePresenter();

        StockSearchInteractor interactor =
                new StockSearchInteractor(dao, presenter);

        interactor.execute(
                new StockSearchInputData("Apple"));

        assertNotNull(presenter.errorMessage);
        assertNull(presenter.outputData);
    }
}
