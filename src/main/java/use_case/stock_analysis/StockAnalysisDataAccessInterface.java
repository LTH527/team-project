package use_case.stock_analysis;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.NavigableMap;

/**
 * Data access interface for retrieving
 * historical stock closing prices.
 */
public interface StockAnalysisDataAccessInterface {

    NavigableMap<LocalDate, BigDecimal>
    getDailyClosingPrices(String symbol)
            throws IOException;
}
