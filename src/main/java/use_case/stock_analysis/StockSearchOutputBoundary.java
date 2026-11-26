package use_case.stock_analysis;

/**
 * Output boundary for stock search.
 */
public interface StockSearchOutputBoundary {

    void prepareSuccessView(StockSearchOutputData outputData);

    void prepareFailView(String errorMessage);
}
