package data_access;

import entity.StockSearchResult;
import use_case.stock_analysis.StockSearchDataAccessInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * In-memory stock search for the MVP.
 */
public class InMemoryStockSearchDataAccessObject
        implements StockSearchDataAccessInterface {

    private final List<StockSearchResult> stocks;

    public InMemoryStockSearchDataAccessObject() {
        stocks = List.of(
                new StockSearchResult("AAPL", "Apple Inc."),
                new StockSearchResult("MSFT", "Microsoft Corporation"),
                new StockSearchResult("IBM", "International Business Machines"),
                new StockSearchResult("GOOGL", "Alphabet Inc."),
                new StockSearchResult("AMZN", "Amazon.com Inc."),
                new StockSearchResult("NVDA", "NVIDIA Corporation"),
                new StockSearchResult("TSLA", "Tesla Inc."),
                new StockSearchResult("META", "Meta Platforms Inc.")
        );
    }

    @Override
    public List<StockSearchResult> searchStocks(String keywords) {

        if (keywords == null || keywords.trim().isEmpty()) {
            return List.of();
        }

        String query = keywords.trim().toLowerCase(Locale.ROOT);

        List<StockSearchResult> matches = new ArrayList<>();

        for (StockSearchResult stock : stocks) {

            String symbol =
                    stock.getSymbol().toLowerCase(Locale.ROOT);

            String companyName =
                    stock.getCompanyName().toLowerCase(Locale.ROOT);

            if (symbol.contains(query)
                    || companyName.contains(query)) {

                matches.add(stock);
            }
        }

        return matches;
    }
}
