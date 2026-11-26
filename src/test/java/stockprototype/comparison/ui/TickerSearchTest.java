package stockprototype.comparison.ui;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import stockprototype.StockListing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TickerSearchTest {
    private final TickerSearch directory = new TickerSearch(List.of(
            new StockListing("TSLA", "Tesla, Inc. Common Stock"),
            new StockListing("TS", "Tenaris"),
            new StockListing("APPLF", "An Example Fund"),
            new StockListing("NVDA", "NVIDIA Corporation Common Stock"),
            new StockListing("AAPL", "Apple Inc. Common Stock"),
            new StockListing("ATMF", "Example / Tesla Fund"),
            new StockListing("NVD", "Example NVIDIA Fund"),
            new StockListing("ABC", "Examples of betesla anywhere")));

    @Test
    void clickingShowsTheFullDirectoryAndSingleLettersKeepTickerPrefixBehavior() {
        assertEquals(List.of("AAPL", "ABC", "APPLF", "ATMF", "NVD", "NVDA", "TS", "TSLA"), symbols(" "));
        assertEquals(List.of("NVD", "NVDA"), symbols("n"));
    }

    @Test
    void exactTickersAndTickerPrefixesLeadThenNamesAndOtherWords() {
        assertEquals(List.of("NVD", "NVDA"), symbols(" nvd "));
        assertEquals(List.of("TS", "TSLA"), symbols("ts"));
        assertEquals(List.of("APPLF", "AAPL"), symbols("appl"));
        assertEquals(List.of("TSLA", "ATMF"), symbols("TesLa"));
        assertEquals(List.of("NVDA", "NVD"), symbols("NVIDIA"));
        assertEquals(List.of("AAPL"), symbols("Apple inc"));
        assertTrue(symbols("esla").isEmpty());
    }

    private List<String> symbols(String query) {
        return directory.find(query).stream().map(listing -> listing.symbol).collect(Collectors.toList());
    }
}
