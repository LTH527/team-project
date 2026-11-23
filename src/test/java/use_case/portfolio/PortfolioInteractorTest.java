package use_case.portfolio;

import entity.Portfolio;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PortfolioInteractorTest {
    private static final class InMemoryPortfolioDataAccess
            implements PortfolioDataAccessInterface {
        private final Map<String, Portfolio> portfolios = new HashMap<>();
        private boolean shouldFailOnSave;

        @Override
        public Portfolio loadPortfolio(String username) {
            Portfolio portfolio = portfolios.get(username);
            if (portfolio == null) {
                return null;
            }
            return copyPortfolio(portfolio);
        }

        @Override
        public void savePortfolio(String username, Portfolio portfolio) {
            if (shouldFailOnSave) {
                throw new RuntimeException("Database save failed");
            }
            portfolios.put(username, copyPortfolio(portfolio));
        }

        public void setShouldFailOnSave(boolean shouldFailOnSave) {
            this.shouldFailOnSave = shouldFailOnSave;
        }

        private Portfolio copyPortfolio(Portfolio portfolio) {
            return Portfolio.restore(
                    Portfolio.DEFAULT_INITIAL_BALANCE,
                    portfolio.getAvailableCash(),
                    portfolio.getHoldings(),
                    portfolio.getTransactions());
        }
    }

    private static final class RecordingOutputBoundary implements PortfolioOutputBoundary {
        private TradeOutputData tradeOutput;
        private PortfolioViewOutputData portfolioOutput;
        private TransactionHistoryOutputData historyOutput;

        @Override
        public void prepareBuyView(TradeOutputData outputData) {
            tradeOutput = outputData;
        }

        @Override
        public void prepareSellView(TradeOutputData outputData) {
            tradeOutput = outputData;
        }

        @Override
        public void preparePortfolioView(PortfolioViewOutputData outputData) {
            portfolioOutput = outputData;
        }

        @Override
        public void prepareTransactionHistoryView(
                TransactionHistoryOutputData outputData) {
            historyOutput = outputData;
        }
    }

    @Test
    public void buysAndSellsAtCurrentPrice() {
        RecordingOutputBoundary presenter = new RecordingOutputBoundary();
        InMemoryPortfolioDataAccess dataAccess = new InMemoryPortfolioDataAccess();
        PortfolioInteractor interactor = new PortfolioInteractor(
                dataAccess,
                ticker -> new BigDecimal("100"),
                presenter);

        interactor.execute(new PortfolioInputData(
                "user", "AAPL", 10, PortfolioInputData.Operation.BUY));

        assertTrue(presenter.tradeOutput.isSuccess());
        assertEquals(new BigDecimal("9000.00"), presenter.tradeOutput.getAvailableCash());

        interactor.execute(new PortfolioInputData(
                "user", "AAPL", 5, PortfolioInputData.Operation.SELL));

        assertTrue(presenter.tradeOutput.isSuccess());
        assertEquals(new BigDecimal("9500.00"), presenter.tradeOutput.getAvailableCash());

        assertEquals(2, dataAccess.loadPortfolio("user").getTransactions().size());
    }

    @Test
    public void presentsInsufficientCashAsFailedTrade() {
        RecordingOutputBoundary presenter = new RecordingOutputBoundary();
        PortfolioInteractor interactor = new PortfolioInteractor(
                new InMemoryPortfolioDataAccess(),
                ticker -> new BigDecimal("200"),
                presenter);

        interactor.execute(new PortfolioInputData(
                "user", "AAPL", 100, PortfolioInputData.Operation.BUY));

        assertFalse(presenter.tradeOutput.isSuccess());
        assertEquals(new BigDecimal("10000.00"),
                presenter.tradeOutput.getAvailableCash());
        assertEquals("Insufficient available cash", presenter.tradeOutput.getMessage());
    }

    @Test
    public void presentsCurrentPortfolioAndTransactionHistory() {
        RecordingOutputBoundary presenter = new RecordingOutputBoundary();
        PortfolioInteractor interactor = new PortfolioInteractor(
                new InMemoryPortfolioDataAccess(),
                ticker -> new BigDecimal("120"),
                presenter);

        interactor.execute(new PortfolioInputData(
                "user", "AAPL", 10, PortfolioInputData.Operation.BUY));
        interactor.execute(new PortfolioInputData(
                "user", PortfolioInputData.Operation.VIEW));
        interactor.execute(new PortfolioInputData(
                "user", PortfolioInputData.Operation.TRANSACTION_HISTORY));

        assertEquals(new BigDecimal("10000.00"), presenter.portfolioOutput.getPortfolioValue());
        assertEquals(new BigDecimal("0.00"), presenter.portfolioOutput.getProfitLoss());
        assertEquals(1, presenter.portfolioOutput.getHoldings().size());
        assertEquals(new BigDecimal("120"),
                presenter.portfolioOutput.getHoldings().get(0).getCurrentPrice());
        assertNotNull(presenter.historyOutput);
        assertEquals(1, presenter.historyOutput.getTransactions().size());
    }

    @Test
    public void usesSeparatePortfolioForEachUsername() {
        RecordingOutputBoundary presenter = new RecordingOutputBoundary();
        PortfolioInteractor interactor = new PortfolioInteractor(
                new InMemoryPortfolioDataAccess(),
                ticker -> new BigDecimal("10"),
                presenter);

        interactor.execute(new PortfolioInputData(
                "alice", "AAPL", 1, PortfolioInputData.Operation.BUY));
        interactor.execute(new PortfolioInputData(
                "bob", "AAPL", 1, PortfolioInputData.Operation.SELL));

        assertFalse(presenter.tradeOutput.isSuccess());
        assertEquals(new BigDecimal("10000.00"),
                presenter.tradeOutput.getAvailableCash());
    }

    @Test
    public void presentsStockApiFailureAsFailedTrade() {
        RecordingOutputBoundary presenter = new RecordingOutputBoundary();
        InMemoryPortfolioDataAccess dataAccess = new InMemoryPortfolioDataAccess();
        PortfolioInteractor interactor = new PortfolioInteractor(
                dataAccess,
                ticker -> {
                    throw new IOException("Stock API unavailable");
                },
                presenter);

        interactor.execute(new PortfolioInputData(
                "user", "AAPL", 1, PortfolioInputData.Operation.BUY));

        assertFalse(presenter.tradeOutput.isSuccess());
        assertEquals("Stock API unavailable", presenter.tradeOutput.getMessage());
        assertEquals(new BigDecimal("10000.00"),
                presenter.tradeOutput.getAvailableCash());
        assertNull(dataAccess.loadPortfolio("user"));
    }

    @Test
    public void presentsSellingMoreSharesThanOwnedAsFailedTrade() {
        RecordingOutputBoundary presenter = new RecordingOutputBoundary();
        InMemoryPortfolioDataAccess dataAccess = new InMemoryPortfolioDataAccess();
        Portfolio portfolio = new Portfolio();
        portfolio.buy("AAPL", 2, new BigDecimal("100.00"));
        dataAccess.savePortfolio("user", portfolio);

        PortfolioInteractor interactor = new PortfolioInteractor(
                dataAccess,
                ticker -> new BigDecimal("120"),
                presenter);

        interactor.execute(new PortfolioInputData(
                "user", "AAPL", 3, PortfolioInputData.Operation.SELL));

        assertFalse(presenter.tradeOutput.isSuccess());
        assertEquals("Cannot sell more shares than are held",
                presenter.tradeOutput.getMessage());
        assertEquals(new BigDecimal("9800.00"),
                presenter.tradeOutput.getAvailableCash());
    }

    @Test
    public void keepsLastKnownPriceWhenStockApiFailsDuringView() {
        RecordingOutputBoundary presenter = new RecordingOutputBoundary();
        InMemoryPortfolioDataAccess dataAccess = new InMemoryPortfolioDataAccess();
        Portfolio portfolio = new Portfolio();
        portfolio.buy("AAPL", 2, new BigDecimal("100.00"));
        dataAccess.savePortfolio("user", portfolio);

        PortfolioInteractor interactor = new PortfolioInteractor(
                dataAccess,
                ticker -> {
                    throw new IOException("Stock API unavailable");
                },
                presenter);

        interactor.execute(new PortfolioInputData(
                "user", PortfolioInputData.Operation.VIEW));

        assertNotNull(presenter.portfolioOutput);
        assertEquals(new BigDecimal("100.00"),
                presenter.portfolioOutput.getHoldings().get(0).getCurrentPrice());
    }

    @Test
    public void presentsBuyFailureWhenDatabaseSaveFails() {
        InMemoryPortfolioDataAccess dataAccess = new InMemoryPortfolioDataAccess();
        dataAccess.setShouldFailOnSave(true);
        RecordingOutputBoundary presenter = new RecordingOutputBoundary();

        PortfolioInteractor interactor = new PortfolioInteractor(
                dataAccess,
                ticker -> new BigDecimal("100.00"),
                presenter);

        interactor.execute(new PortfolioInputData(
                "Alice", "AAPL", 1, PortfolioInputData.Operation.BUY));

        assertFalse(presenter.tradeOutput.isSuccess());
        assertEquals(
                "Could not save your portfolio. Please try again.",
                presenter.tradeOutput.getMessage());
        assertEquals(new BigDecimal("10000.00"),
                presenter.tradeOutput.getAvailableCash());
        assertNull(dataAccess.loadPortfolio("Alice"));
    }

    @Test
    public void presentsSellFailureWhenDatabaseSaveFails() {
        InMemoryPortfolioDataAccess dataAccess = new InMemoryPortfolioDataAccess();
        Portfolio portfolio = new Portfolio();
        portfolio.buy("AAPL", 2, new BigDecimal("100.00"));
        dataAccess.savePortfolio("Alice", portfolio);
        dataAccess.setShouldFailOnSave(true);
        RecordingOutputBoundary presenter = new RecordingOutputBoundary();

        PortfolioInteractor interactor = new PortfolioInteractor(
                dataAccess,
                ticker -> new BigDecimal("120.00"),
                presenter);

        interactor.execute(new PortfolioInputData(
                "Alice", "AAPL", 1, PortfolioInputData.Operation.SELL));

        assertFalse(presenter.tradeOutput.isSuccess());
        assertEquals(
                "Could not save your portfolio. Please try again.",
                presenter.tradeOutput.getMessage());
        assertEquals(new BigDecimal("9800.00"),
                presenter.tradeOutput.getAvailableCash());

        Portfolio savedPortfolio = dataAccess.loadPortfolio("Alice");
        assertEquals(2, savedPortfolio.getHoldings().get("AAPL").getQuantity());
        assertEquals(new BigDecimal("9800.00"),
                savedPortfolio.getAvailableCash());
    }

    @Test
    public void stillPresentsPortfolioWhenDatabaseSaveFailsDuringView() {
        InMemoryPortfolioDataAccess dataAccess = new InMemoryPortfolioDataAccess();
        Portfolio portfolio = new Portfolio();
        portfolio.buy("AAPL", 2, new BigDecimal("100.00"));
        dataAccess.savePortfolio("Alice", portfolio);
        dataAccess.setShouldFailOnSave(true);
        RecordingOutputBoundary presenter = new RecordingOutputBoundary();

        PortfolioInteractor interactor = new PortfolioInteractor(
                dataAccess,
                ticker -> new BigDecimal("120.00"),
                presenter);

        interactor.execute(new PortfolioInputData(
                "Alice", PortfolioInputData.Operation.VIEW));

        assertNotNull(presenter.portfolioOutput);
        assertEquals(new BigDecimal("10040.00"),
                presenter.portfolioOutput.getPortfolioValue());
        assertEquals(new BigDecimal("40.00"),
                presenter.portfolioOutput.getProfitLoss());
    }

    @Test
    public void handlesSimultaneousBuyOperationsWithoutLostUpdates() throws Exception {
        final int numberOfThreads = 10;
        final int sharesPerBuy = 10;

        RecordingOutputBoundary presenter = new RecordingOutputBoundary();
        InMemoryPortfolioDataAccess dataAccess = new InMemoryPortfolioDataAccess();

        PortfolioInteractor interactor = new PortfolioInteractor(
                dataAccess,
                ticker -> new BigDecimal("100.00"),
                presenter);

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch ready = new CountDownLatch(numberOfThreads);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<?>[] futures = new Future<?>[numberOfThreads];

            for (int i = 0; i < numberOfThreads; i++) {
                futures[i] = executor.submit(() -> {
                    ready.countDown();

                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to start");
                    }

                    interactor.execute(new PortfolioInputData(
                            "user",
                            "AAPL",
                            sharesPerBuy,
                            PortfolioInputData.Operation.BUY));
                    return null;
                });
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS),
                    "All threads should be ready to start");

            start.countDown();

            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        Portfolio savedPortfolio = dataAccess.loadPortfolio("user");

        assertNotNull(savedPortfolio);
        assertEquals(100,
                savedPortfolio.getHoldings().get("AAPL").getQuantity());
        assertEquals(new BigDecimal("0.00"),
                savedPortfolio.getAvailableCash());
        assertEquals(10,
                savedPortfolio.getTransactions().size());
    }

    @Test
    public void handlesSimultaneousBuysWithoutOverspendingCash() throws Exception {
        final int numberOfThreads = 10;
        final int sharesPerBuy = 20;

        RecordingOutputBoundary presenter = new RecordingOutputBoundary();
        InMemoryPortfolioDataAccess dataAccess = new InMemoryPortfolioDataAccess();

        PortfolioInteractor interactor = new PortfolioInteractor(
                dataAccess,
                ticker -> new BigDecimal("100.00"),
                presenter);

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch ready = new CountDownLatch(numberOfThreads);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<?>[] futures = new Future<?>[numberOfThreads];

            for (int i = 0; i < numberOfThreads; i++) {
                futures[i] = executor.submit(() -> {
                    ready.countDown();

                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to start");
                    }

                    interactor.execute(new PortfolioInputData(
                            "user",
                            "AAPL",
                            sharesPerBuy,
                            PortfolioInputData.Operation.BUY));
                    return null;
                });
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS),
                    "All threads should be ready to start");

            start.countDown();

            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        Portfolio savedPortfolio = dataAccess.loadPortfolio("user");

        assertNotNull(savedPortfolio);
        assertEquals(100,
                savedPortfolio.getHoldings().get("AAPL").getQuantity());
        assertEquals(new BigDecimal("0.00"),
                savedPortfolio.getAvailableCash());
        assertEquals(5,
                savedPortfolio.getTransactions().size());
    }

    @Test
    public void handlesSimultaneousSellsWithoutOversellingShares() throws Exception {
        final int numberOfThreads = 10;
        final int sharesPerSell = 20;

        RecordingOutputBoundary presenter = new RecordingOutputBoundary();
        InMemoryPortfolioDataAccess dataAccess = new InMemoryPortfolioDataAccess();

        PortfolioInteractor interactor = new PortfolioInteractor(
                dataAccess,
                ticker -> new BigDecimal("100.00"),
                presenter);

        interactor.execute(new PortfolioInputData(
                "user",
                "AAPL",
                100,
                PortfolioInputData.Operation.BUY));

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch ready = new CountDownLatch(numberOfThreads);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<?>[] futures = new Future<?>[numberOfThreads];

            for (int i = 0; i < numberOfThreads; i++) {
                futures[i] = executor.submit(() -> {
                    ready.countDown();

                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to start");
                    }

                    interactor.execute(new PortfolioInputData(
                            "user",
                            "AAPL",
                            sharesPerSell,
                            PortfolioInputData.Operation.SELL));
                    return null;
                });
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS),
                    "All threads should be ready to start");

            start.countDown();

            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        Portfolio savedPortfolio = dataAccess.loadPortfolio("user");

        assertNotNull(savedPortfolio);
        assertFalse(savedPortfolio.getHoldings().containsKey("AAPL"));
        assertEquals(new BigDecimal("10000.00"),
                savedPortfolio.getAvailableCash());
        assertEquals(6,
                savedPortfolio.getTransactions().size());
    }
}
