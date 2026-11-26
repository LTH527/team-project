package stockprototype.comparison;

import java.io.IOException;

/** Input boundary for comparing the historical performance of two stocks. */
public interface CompareStocks {
    /**
     * Compares the most recent shared closes, using the first shared close as each stock's baseline.
     *
     * @param first the first stock symbol
     * @param second a different stock symbol
     * @param maxSessions maximum number of shared dates, or zero for all available dates
     * @return both price histories and the metrics computed over the selected dates
     * @throws IOException when a price source cannot provide a history
     * @throws IllegalArgumentException when the inputs or available prices cannot support a comparison
     */
    ComparisonResult compare(String first, String second, int maxSessions) throws IOException;
}
