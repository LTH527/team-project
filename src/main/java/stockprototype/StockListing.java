package stockprototype;

/** A stock directory entry. Providers can omit the display name. */
public final class StockListing {
    public final String symbol;
    public final String name;

    public StockListing(String symbol, String name) {
        this.symbol = StockSymbol.normalize(symbol);
        this.name = name == null ? "" : name.strip();
    }
}
