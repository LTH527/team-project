package stockprototype;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONException;

/** Keeps the searchable ticker directory between application runs. */
public final class CachedStockDirectory implements StockSymbolDirectory {
    private static final Duration MAX_AGE = Duration.ofDays(1);
    private final StockSymbolDirectory source;
    private final Path file;

    public static CachedStockDirectory forAlpaca(StockSymbolDirectory source) {
        Path file = Path.of(System.getProperty("user.home"), ".cache", "csc207-stock-comparison",
                "alpaca-listings-v1.json");
        return new CachedStockDirectory(source, file);
    }

    CachedStockDirectory(StockSymbolDirectory source, Path file) {
        this.source = source;
        this.file = file;
    }

    @Override
    public List<String> getSymbols() throws IOException {
        return getListings().stream().map(listing -> listing.symbol).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<StockListing> getListings() throws IOException {
        return loadListings(cached -> { });
    }

    @Override
    public List<StockListing> loadListings(Consumer<List<StockListing>> onCached) throws IOException {
        List<StockListing> cached = readCache();
        if (!cached.isEmpty()) {
            if (isFresh()) {
                return cached;
            }
            onCached.accept(cached);
        }
        try {
            List<StockListing> listings = List.copyOf(source.getListings());
            if (listings.isEmpty()) {
                return cached;
            }
            saveCache(listings);
            return listings;
        } catch (IOException exception) {
            if (cached.isEmpty()) {
                throw exception;
            }
            return cached;
        }
    }

    private boolean isFresh() {
        try {
            Instant updated = Files.getLastModifiedTime(file).toInstant();
            Duration age = Duration.between(updated, Instant.now());
            return !age.isNegative() && age.compareTo(MAX_AGE) < 0;
        } catch (IOException | SecurityException exception) {
            return false;
        }
    }

    private List<StockListing> readCache() {
        try {
            JSONArray rows = new JSONArray(Files.readString(file));
            List<StockListing> listings = new ArrayList<>(rows.length());
            for (int i = 0; i < rows.length(); i++) {
                JSONArray row = rows.getJSONArray(i);
                listings.add(new StockListing(row.getString(0), row.getString(1)));
            }
            return List.copyOf(listings);
        } catch (IOException | JSONException | IllegalArgumentException | SecurityException exception) {
            return List.of();
        }
    }

    private void saveCache(List<StockListing> listings) {
        try {
            Path directory = Files.createDirectories(file.getParent());
            Path temp = Files.createTempFile(directory, "listings-", ".tmp");
            try {
                JSONArray rows = new JSONArray();
                for (StockListing listing : listings) {
                    rows.put(new JSONArray().put(listing.symbol).put(listing.name));
                }
                Files.writeString(temp, rows.toString());
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException | SecurityException ignored) {
            // The fetched directory is still usable for this run when persistence is unavailable.
        }
    }
}
