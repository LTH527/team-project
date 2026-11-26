package use_case.stock_analysis;

/**
 * Output boundary for stock analysis.
 */
public interface StockAnalysisOutputBoundary {

    void prepareSuccessView(StockAnalysisOutputData outputData);

    void prepareFailView(String errorMessage);
}
