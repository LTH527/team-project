package app;

import data_access.portfolio.PortfolioDataAccessObject;
import data_access.portfolio.StockPriceDataAccessAdapter;
import data_access.alpaca.AlpacaStockDataAccess;
import interface_adapter.portfolio.PortfolioController;
import presentation.portfolio.PortfolioPresenter;
import presentation.portfolio.PortfolioViewModel;
import stockprototype.AlphaVantageClient;
import use_case.portfolio.PortfolioDataAccessInterface;
import use_case.portfolio.PortfolioInteractor;
import use_case.portfolio.PortfolioOutputBoundary;
import use_case.portfolio.StockPriceDataAccessInterface;
import use_case.portfolio.PortfolioInputData;
import view.PortfolioView;

import java.sql.SQLException;

public final class PortfolioAppBuilder {

    private PortfolioAppBuilder() {
    }

    public static PortfolioView createPortfolioView(String username)
            throws SQLException {

        PortfolioDataAccessInterface portfolioDAO =
                new PortfolioDataAccessObject();

        String alpacaKey = firstEnvironmentValue(
                "APCA_API_KEY_ID",
                "ALPACA_API_KEY");
        String alpacaSecret = firstEnvironmentValue(
                "APCA_API_SECRET_KEY",
                "ALPACA_API_SECRET");
        StockPriceDataAccessInterface priceDataAccess;

        if (alpacaKey != null && !alpacaKey.trim().isEmpty()
                && alpacaSecret != null && !alpacaSecret.trim().isEmpty()) {
            priceDataAccess =
                    new AlpacaStockDataAccess(alpacaKey, alpacaSecret);
        } else {
            String alphaVantageKey =
                    System.getenv("ALPHA_VANTAGE_API_KEY");
            AlphaVantageClient stockDataSource =
                    new AlphaVantageClient(alphaVantageKey);
            priceDataAccess =
                    new StockPriceDataAccessAdapter(stockDataSource);
        }

        PortfolioViewModel viewModel = new PortfolioViewModel();
        PortfolioOutputBoundary presenter =
                new PortfolioPresenter(viewModel);

        PortfolioInteractor interactor =
                new PortfolioInteractor(
                        portfolioDAO,
                        priceDataAccess,
                        presenter);

        PortfolioController controller =
                new PortfolioController(interactor);

        if (priceDataAccess instanceof AlpacaStockDataAccess) {
            AlpacaStockDataAccess alpacaDataAccess =
                    (AlpacaStockDataAccess) priceDataAccess;
            alpacaDataAccess.setPriceUpdateListener((ticker, price) ->
                    interactor.execute(new PortfolioInputData(
                            username,
                            PortfolioInputData.Operation.VIEW)));
        }

        return new PortfolioView(controller, viewModel, username);
    }

    private static String firstEnvironmentValue(String... names) {
        for (String name : names) {
            String value = System.getenv(name);
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }
}
