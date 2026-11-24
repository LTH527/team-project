package use_case.stock_analysis;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.Objects;

/**
 * Handles the business logic for analyzing a single stock.
 */
public class StockAnalysisInteractor implements StockAnalysisInputBoundary {

    private final StockAnalysisDataAccessInterface dataAccess;
    private final StockAnalysisOutputBoundary presenter;

    public StockAnalysisInteractor(
            StockAnalysisDataAccessInterface dataAccess,
            StockAnalysisOutputBoundary presenter) {
        this.dataAccess = Objects.requireNonNull(dataAccess);
        this.presenter = Objects.requireNonNull(presenter);
    }

    @Override
    public void execute(StockAnalysisInputData inputData) {

        // Step 1: Validate the user's input.
        if (inputData == null || inputData.getSymbol() == null
                || inputData.getSymbol().trim().isEmpty()) {
            presenter.prepareFailView("Please enter a stock symbol.");
            return;
        }

        String symbol = inputData.getSymbol()
                .trim()
                .toUpperCase(Locale.ROOT);

        // Step 2: Validate the stock symbol format.
        if (!symbol.matches("[A-Z0-9][A-Z0-9.\\-]{0,29}")) {
            presenter.prepareFailView("Invalid stock symbol format.");
            return;
        }

        // Step 3: Retrieve historical stock prices.
        try {
            NavigableMap<LocalDate, BigDecimal> prices =
                    dataAccess.getDailyClosingPrices(symbol);

            // Step 4: Handle missing stock data.
            if (prices == null || prices.isEmpty()) {
                presenter.prepareFailView(
                        "No historical data found for this stock.");
                return;
            }

            // Step 5: Prepare successful output.
            StockAnalysisOutputData outputData =
                    new StockAnalysisOutputData(symbol, prices);

            presenter.prepareSuccessView(outputData);

        }
        catch (IllegalArgumentException exception) {
            presenter.prepareFailView("Invalid stock symbol.");
        }
        catch (IOException exception) {
            presenter.prepareFailView(
                    "Unable to load stock data. Please try again later.");
        }
    }
}
