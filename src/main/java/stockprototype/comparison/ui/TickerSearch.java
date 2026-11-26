package stockprototype.comparison.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import stockprototype.StockListing;

/** Alphabetical directory with ticker prefixes first and issuer name matches next. */
final class TickerSearch {
    private final List<StockListing> listings;
    private final String[] names;

    TickerSearch(List<StockListing> source) {
        List<StockListing> sorted = new ArrayList<>(source);
        sorted.sort(Comparator.comparing(listing -> listing.symbol));
        listings = Collections.unmodifiableList(sorted);
        names = new String[sorted.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = sorted.get(i).name.toUpperCase(Locale.ROOT);
        }
    }

    List<StockListing> find(String text) {
        String query = text.strip().toUpperCase(Locale.ROOT);
        if (query.isEmpty()) {
            return listings;
        }
        int start = lowerBound(query);
        // Symbols use ASCII letters, digits, dots and hyphens, all less than this upper bound.
        int end = lowerBound(query + '\uffff');
        List<StockListing> tickerMatches = listings.subList(start, end);
        if (query.length() == 1) {
            return tickerMatches;
        }

        List<StockListing> matches = new ArrayList<>(tickerMatches);
        List<StockListing> laterWords = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            if (i >= start && i < end) {
                continue;
            }
            if (names[i].startsWith(query)) {
                matches.add(listings.get(i));
            } else if (startsLaterWord(names[i], query)) {
                laterWords.add(listings.get(i));
            }
        }
        matches.addAll(laterWords);
        return matches;
    }

    private int lowerBound(String text) {
        int lo = 0;
        int hi = listings.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (listings.get(mid).symbol.compareTo(text) < 0) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    private static boolean startsLaterWord(String name, String query) {
        for (int at = name.indexOf(query, 1); at >= 0; at = name.indexOf(query, at + 1)) {
            if (!Character.isLetterOrDigit(name.charAt(at - 1))) {
                return true;
            }
        }
        return false;
    }
}
