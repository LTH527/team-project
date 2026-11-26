package stockprototype;

import java.util.Locale;
import java.util.regex.Pattern;

public final class StockSymbol {
    private static final Pattern SYMBOL = Pattern.compile("[A-Z0-9][A-Z0-9.\\-]{0,29}");

    private StockSymbol() { }

    public static String normalize(String symbol) {
        if (symbol == null) {
            throw new IllegalArgumentException("Enter a stock symbol.");
        }
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        if (!isSupported(normalized)) {
            throw new IllegalArgumentException("Enter a stock symbol using letters, digits, dots or hyphens.");
        }
        return normalized;
    }

    static boolean isSupported(String symbol) {
        return SYMBOL.matcher(symbol).matches();
    }
}
