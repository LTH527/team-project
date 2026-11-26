package app;

import data_access.InMemoryStockSearchDataAccessObject;
import data_access.StockAnalysisDataAccessAdapter;

import interface_adapter.stock_analysis.StockAnalysisController;
import interface_adapter.stock_analysis.StockAnalysisPresenter;
import interface_adapter.stock_analysis.StockAnalysisViewModel;
import interface_adapter.stock_analysis.StockSearchController;
import interface_adapter.stock_analysis.StockSearchPresenter;
import interface_adapter.stock_analysis.StockSearchViewModel;

import stockprototype.AlpacaClient;

import use_case.stock_analysis.StockAnalysisDataAccessInterface;
import use_case.stock_analysis.StockAnalysisInputBoundary;
import use_case.stock_analysis.StockAnalysisInteractor;
import use_case.stock_analysis.StockSearchInputBoundary;
import use_case.stock_analysis.StockSearchInteractor;

import view.StockAnalysisView;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Standalone launcher for the stock analysis feature.
 */
public final class StockAnalysisApp {

    private StockAnalysisApp() {
    }

    public static void main(String[] args) {

        String keyId = System.getenv("APCA_API_KEY_ID");
        String secretKey = System.getenv("APCA_API_SECRET_KEY");

        if (keyId == null || keyId.isBlank()
                || secretKey == null || secretKey.isBlank()) {
            System.err.println(
                    "Please configure Alpaca API credentials.");
            return;
        }

        SwingUtilities.invokeLater(() -> {

            // Step 1: Create the Stock Analysis ViewModel.
            StockAnalysisViewModel viewModel =
                    new StockAnalysisViewModel();

            // Step 2: Create the Stock Analysis Presenter.
            StockAnalysisPresenter presenter =
                    new StockAnalysisPresenter(viewModel);

            // Step 3: Connect the Alpaca API.
            AlpacaClient apiClient =
                    AlpacaClient.fromEnvironment();

            StockAnalysisDataAccessInterface dataAccess =
                    new StockAnalysisDataAccessAdapter(apiClient);

            // Step 4: Create the Stock Analysis Interactor.
            StockAnalysisInputBoundary interactor =
                    new StockAnalysisInteractor(
                            dataAccess, presenter);

            // Step 5: Create the Stock Analysis Controller.
            StockAnalysisController controller =
                    new StockAnalysisController(interactor);

            // Step 6: Create the Stock Search ViewModel.
            StockSearchViewModel searchViewModel =
                    new StockSearchViewModel();

            // Step 7: Create the Stock Search Presenter.
            StockSearchPresenter searchPresenter =
                    new StockSearchPresenter(searchViewModel);

            // Step 8: Create the In-Memory Search Data Access.
            InMemoryStockSearchDataAccessObject searchDataAccess =
                    new InMemoryStockSearchDataAccessObject();

            // Step 9: Create the Stock Search Interactor.
            StockSearchInputBoundary searchInteractor =
                    new StockSearchInteractor(
                            searchDataAccess, searchPresenter);

            // Step 10: Create the Stock Search Controller.
            StockSearchController searchController =
                    new StockSearchController(searchInteractor);

            // Step 11: Create the Stock Analysis View.
            StockAnalysisView view =
                    new StockAnalysisView(
                            controller,
                            viewModel,
                            searchController,
                            searchViewModel
                    );

            // Step 12: Display the application.
            JFrame frame = new JFrame("Stock Analysis");

            frame.setDefaultCloseOperation(
                    JFrame.EXIT_ON_CLOSE);

            frame.setContentPane(view);
            frame.setSize(900, 650);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
