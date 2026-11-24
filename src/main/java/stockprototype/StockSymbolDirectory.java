package stockprototype;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Supplies the stock symbols available for suggestions in a comparison. */
@FunctionalInterface
public interface StockSymbolDirectory {
    List<String> getSymbols() throws IOException;

    /** Includes display names when the directory provides them. */
    default List<StockListing> getListings() throws IOException {
        return getSymbols().stream()
                .map(symbol -> new StockListing(symbol, ""))
                .collect(Collectors.toUnmodifiableList());
    }

    /** A directory with a cache may supply older listings while refreshing the catalog. */
    default List<StockListing> loadListings(Consumer<List<StockListing>> onCached) throws IOException {
        return getListings();
    }
}
