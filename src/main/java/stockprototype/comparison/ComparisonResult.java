package stockprototype.comparison;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import stockprototype.StockTrade;

/** Paired raw closes and comparison metrics for exactly the same trading dates. */
public final class ComparisonResult {
    public final String firstSymbol;
    public final String secondSymbol;
    public final List<Day> days;
    public final BigDecimal firstChange;
    public final BigDecimal secondChange;
    public final BigDecimal firstProjection;
    public final BigDecimal secondProjection;
    private final double correlation;
    private final StockTrade firstTrade;
    private final StockTrade secondTrade;

    ComparisonResult(String firstSymbol, String secondSymbol, List<Day> days,
                     BigDecimal firstChange, BigDecimal secondChange, BigDecimal firstProjection,
                     BigDecimal secondProjection, double correlation,
                     StockTrade firstTrade, StockTrade secondTrade) {
        this.firstSymbol = firstSymbol;
        this.secondSymbol = secondSymbol;
        this.days = List.copyOf(days);
        this.firstChange = firstChange;
        this.secondChange = secondChange;
        this.firstProjection = firstProjection;
        this.secondProjection = secondProjection;
        this.correlation = correlation;
        this.firstTrade = firstTrade;
        this.secondTrade = secondTrade;
    }

    public OptionalDouble correlation() {
        return Double.isNaN(correlation) ? OptionalDouble.empty() : OptionalDouble.of(correlation);
    }

    public Optional<StockTrade> firstTrade() {
        return Optional.ofNullable(firstTrade);
    }

    public Optional<StockTrade> secondTrade() {
        return Optional.ofNullable(secondTrade);
    }

    /** Closing prices and their rebased index values for one shared trading date. */
    public static final class Day {
        public final LocalDate date;
        public final BigDecimal firstClose;
        public final BigDecimal secondClose;
        public final BigDecimal firstIndex;
        public final BigDecimal secondIndex;

        Day(LocalDate date, BigDecimal firstClose, BigDecimal secondClose,
            BigDecimal firstIndex, BigDecimal secondIndex) {
            this.date = date;
            this.firstClose = firstClose;
            this.secondClose = secondClose;
            this.firstIndex = firstIndex;
            this.secondIndex = secondIndex;
        }
    }
}
