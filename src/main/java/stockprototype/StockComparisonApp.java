package stockprototype;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import stockprototype.comparison.ComparisonInteractor;
import stockprototype.comparison.ui.ComparisonPanel;

/** Starts the two-stock comparison screen. */
public final class StockComparisonApp {
    private StockComparisonApp() { }

    /**
     * Opens the standalone stock comparison window.
     *
     * @param args optional --demo or --live price mode
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                ComparisonPanel comparison = createPanel(args);
                JFrame frame = new JFrame("Stock comparison");
                frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                frame.setContentPane(comparison);
                frame.pack();
                frame.setLocationByPlatform(true);
                frame.setVisible(true);
            } catch (IllegalArgumentException exception) {
                JOptionPane.showMessageDialog(null, exception.getMessage(),
                        "Stock comparison", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    /** Wires the same price source and use case for the team app and the standalone screen. */
    public static ComparisonPanel createPanel(String[] args) {
        boolean live = selectLiveMode(args);
        StockDataSource prices = live ? AlpacaClient.fromEnvironment() : new SampleStockDataSource();
        StockSymbolDirectory directory = live ? CachedStockDirectory.forAlpaca(prices) : prices;
        return new ComparisonPanel(new ComparisonInteractor(prices), live, directory);
    }

    private static boolean selectLiveMode(String[] args) {
        if (args.length == 0) {
            return hasEnvironmentValue("APCA_API_KEY_ID") || hasEnvironmentValue("APCA_API_SECRET_KEY");
        }
        if (args.length == 1 && "--demo".equals(args[0])) {
            return false;
        }
        if (args.length == 1 && "--live".equals(args[0])) {
            return true;
        }
        throw new IllegalArgumentException(
                "Use --demo for sample prices or --live for Alpaca.");
    }

    private static boolean hasEnvironmentValue(String name) {
        String value = System.getenv(name);
        return value != null && !value.isBlank();
    }
}
