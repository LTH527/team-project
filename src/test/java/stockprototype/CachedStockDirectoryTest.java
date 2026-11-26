package stockprototype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CachedStockDirectoryTest {
    @TempDir
    Path directory;

    private final StockSymbolDirectory firstSource = new StockSymbolDirectory() {
        @Override
        public List<String> getSymbols() {
            return List.of("ACME");
        }

        @Override
        public List<StockListing> getListings() {
            return List.of(new StockListing("ACME", "Acme \"A\" & Co."));
        }
    };

    @Test
    void warmStartReadsTickerAndNameWithoutContactingTheProvider() throws IOException {
        Path file = directory.resolve("stock-dir/listings.json");
        new CachedStockDirectory(firstSource, file).getListings();
        CachedStockDirectory restarted = new CachedStockDirectory(() -> fail("Unexpected download"), file);
        List<StockListing> listings = restarted.loadListings(cached -> fail("Fresh listing should be final"));
        assertEquals(1, listings.size());
        assertEquals("ACME", listings.get(0).symbol);
        assertEquals("Acme \"A\" & Co.", listings.get(0).name);
    }

    @Test
    void expiredDirectoryIsAvailableBeforeRefreshAndSurvivesAnOfflineRun() throws IOException {
        Path file = directory.resolve("listings.json");
        new CachedStockDirectory(firstSource, file).getListings();
        Files.setLastModifiedTime(file, FileTime.from(Instant.now().minus(Duration.ofHours(25))));
        List<String> steps = new ArrayList<>();
        StockSymbolDirectory offline = () -> {
            steps.add("download");
            throw new IOException("offline");
        };
        CachedStockDirectory stale = new CachedStockDirectory(offline, file);
        List<StockListing> fallback = stale.loadListings(previous -> steps.add(previous.get(0).symbol));
        assertEquals(List.of("ACME", "download"), steps);
        assertEquals("ACME", fallback.get(0).symbol);
        assertEquals(List.of("TSLA"), new CachedStockDirectory(() -> List.of("TSLA"), file).getSymbols());
        assertEquals(List.of("TSLA"), new CachedStockDirectory(offline, file).getSymbols());
        assertEquals(List.of("ACME", "download"), steps);
    }

    @Test
    void damagedCacheIsRebuiltAndAnUnwritableCacheStillReturnsResults() throws IOException {
        Path file = directory.resolve("listings.json");
        Files.writeString(file, "[[\"ACME\"]]");
        CachedStockDirectory repaired = new CachedStockDirectory(firstSource, file);
        assertEquals("ACME", repaired.loadListings(cached -> fail("Damaged cache returned")).get(0).symbol);
        Path impossible = file.resolve("listings.json");
        CachedStockDirectory unwritable = new CachedStockDirectory(firstSource, impossible);
        assertEquals(List.of("ACME"), unwritable.getSymbols());
        assertFalse(Files.exists(impossible));
    }

    @Test
    void missingDirectoryAndNetworkFailurePropagateTheProviderError() {
        CachedStockDirectory missing = new CachedStockDirectory(() -> {
            throw new IOException("offline");
        }, directory.resolve("missing.json"));
        assertEquals("offline", assertThrows(IOException.class, missing::getListings).getMessage());
    }
}
