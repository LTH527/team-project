package interface_adapter.stock_analysis;

import org.junit.jupiter.api.Test;
import use_case.stock_analysis.StockAnalysisInputBoundary;
import use_case.stock_analysis.StockAnalysisInputData;

import static org.junit.jupiter.api.Assertions.*;

class StockAnalysisControllerTest {

    @Test
    void executeTest() {
        FakeInteractor interactor = new FakeInteractor();

        StockAnalysisController controller =
                new StockAnalysisController(interactor);

        controller.execute("AAPL");

        assertNotNull(interactor.receivedInput);
        assertEquals("AAPL",
                interactor.receivedInput.getSymbol());
    }

    private static class FakeInteractor
            implements StockAnalysisInputBoundary {

        private StockAnalysisInputData receivedInput;

        @Override
        public void execute(StockAnalysisInputData inputData) {
            receivedInput = inputData;
        }
    }
}
