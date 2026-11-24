package use_case.stock_analysis;

import entity.StockSearchResult;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Handles the business logic for stock searches.
 */
public class StockSearchInteractor
        implements StockSearchInputBoundary {

    private final StockSearchDataAccessInterface dataAccess;
    private final StockSearchOutputBoundary presenter;

    public StockSearchInteractor(
            StockSearchDataAccessInterface dataAccess,
            StockSearchOutputBoundary presenter) {

        this.dataAccess = Objects.requireNonNull(dataAccess);
        this.presenter = Objects.requireNonNull(presenter);
    }

    @Override
    public void execute(StockSearchInputData inputData) {

        // Validate the search keywords.
        if (inputData == null
                || inputData.getKeywords() == null
                || inputData.getKeywords().trim().isEmpty()) {

            presenter.prepareFailView(
                    "Please enter a company name or stock symbol.");
            return;
        }

        String keywords = inputData.getKeywords().trim();

        try {
            // Search for matching stocks.
            List<StockSearchResult> results =
                    dataAccess.searchStocks(keywords);

            // Handle searches with no matches.
            if (results == null || results.isEmpty()) {
                presenter.prepareFailView(
                        "No matching stocks found.");
                return;
            }

            // Return matching stocks to the presenter.
            StockSearchOutputData outputData =
                    new StockSearchOutputData(results);

            presenter.prepareSuccessView(outputData);
        }
        catch (IOException exception) {
            presenter.prepareFailView(
                    "Stock search is unavailable. Please try again.");
        }
    }
}
