package stockprototype;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/** An individual stock trade with its execution time. */
public final class StockTrade {
    public final BigDecimal price;
    public final Instant time;

    public StockTrade(BigDecimal price, Instant time) {
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("A trade price must be positive.");
        }
        this.price = price;
        this.time = Objects.requireNonNull(time);
    }
}
