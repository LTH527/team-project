package data_access;

import stockprototype.StockDataSource;
import use_case.stock_analysis.StockAnalysisDataAccessInterface;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.NavigableMap;
import java.util.Objects;

/**
 * Connects the stock analysis use case
 * to the existing stock data source.
 */
public class StockAnalysisDataAccessAdapter
        implements StockAnalysisDataAccessInterface {

    private final StockDataSource stockDataSource;

    public StockAnalysisDataAccessAdapter(
            StockDataSource stockDataSource) {
        this.stockDataSource =
                Objects.requireNonNull(stockDataSource);
    }

    @Override
    public NavigableMap<LocalDate, BigDecimal>
    getDailyClosingPrices(String symbol)
            throws IOException {

        return stockDataSource.getDailyClosingPrices(symbol);
    }
}
