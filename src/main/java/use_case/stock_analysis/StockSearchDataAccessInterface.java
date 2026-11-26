package use_case.stock_analysis;

import entity.StockSearchResult;

import java.io.IOException;
import java.util.List;

/**
 * Data access interface for searching stocks
 * by company name or ticker symbol.
 */
public interface StockSearchDataAccessInterface {

    List<StockSearchResult> searchStocks(String keywords)
            throws IOException;
}
