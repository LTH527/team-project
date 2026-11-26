package view;

import interface_adapter.stock_analysis.StockAnalysisController;
import interface_adapter.stock_analysis.StockAnalysisState;
import interface_adapter.stock_analysis.StockAnalysisViewModel;
import interface_adapter.stock_analysis.StockSearchController;
import interface_adapter.stock_analysis.StockSearchViewModel;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.Collections;

/**
 * Swing view for analyzing a single stock.
 */
public class StockAnalysisView extends JPanel
        implements PropertyChangeListener {

    private final StockAnalysisController controller;
    private final StockSearchPanel searchPanel;
    private final JLabel statusLabel;
    private final JTextArea resultArea;
    private final StockPriceChartPanel chartPanel;

    public StockAnalysisView(
            StockAnalysisController controller,
            StockAnalysisViewModel viewModel,
            StockSearchController searchController,
            StockSearchViewModel searchViewModel) {

        this.controller = Objects.requireNonNull(controller);
        Objects.requireNonNull(viewModel);

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        searchPanel = new StockSearchPanel(
                searchController,
                searchViewModel,
                this::searchStock,
                this::showSearchError
        );

        statusLabel = new JLabel("Enter a stock symbol.");
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(
                new Font(Font.MONOSPACED, Font.PLAIN, 14));

        JPanel topPanel = new JPanel(
                new BorderLayout(0, 10));
        topPanel.add(searchPanel, BorderLayout.NORTH);
        topPanel.add(statusLabel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        chartPanel = new StockPriceChartPanel();

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                chartPanel,
                new JScrollPane(resultArea)
        );

        splitPane.setResizeWeight(0.65);
        splitPane.setDividerLocation(350);

        add(splitPane, BorderLayout.CENTER);

        viewModel.addPropertyChangeListener(this);
    }


    private void searchStock(String symbol) {

        searchPanel.setLoading(true);
        statusLabel.setText("Loading stock data...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {

            @Override
            protected Void doInBackground() {
                controller.execute(symbol);
                return null;
            }

            @Override
            protected void done() {
                searchPanel.setLoading(false);

                try {
                    get();
                }
                catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showSearchError("Search interrupted.");
                }
                catch (ExecutionException exception) {
                    showSearchError(
                            "Unexpected error while loading stock data.");
                }
            }
        };

        worker.execute();
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (StockAnalysisViewModel.STATE_PROPERTY
                .equals(event.getPropertyName())) {

            StockAnalysisState state =
                    (StockAnalysisState) event.getNewValue();

            SwingUtilities.invokeLater(() -> updateView(state));
        }
    }

    private void showSearchError(String message) {

        statusLabel.setText(message);
        resultArea.setText("");

        chartPanel.setClosingPrices(
                Collections.emptyNavigableMap());
    }

    private void updateView(StockAnalysisState state) {
        if (!state.getErrorMessage().isEmpty()) {
            statusLabel.setText(state.getErrorMessage());
            resultArea.setText("");
            chartPanel.setClosingPrices(Collections.emptyNavigableMap());
            return;
        }

        if (state.getClosingPrices().isEmpty()) {
            statusLabel.setText("No stock data available.");
            resultArea.setText("");
            chartPanel.setClosingPrices(Collections.emptyNavigableMap());
            return;
        }

        chartPanel.setClosingPrices(state.getClosingPrices());

        statusLabel.setText(
                "Analysis loaded: " + state.getSymbol());

        StringBuilder result = new StringBuilder();
        result.append("Symbol: ")
                .append(state.getSymbol()).append("\n");

        result.append("Latest Close: ")
                .append(state.getClosingPrices()
                        .lastEntry().getValue())
                .append("\n");

        result.append("Historical Records: ")
                .append(state.getClosingPrices().size())
                .append("\n\n");

        result.append("Recent Closing Prices:\n");

        int count = 0;

        for (Map.Entry<LocalDate, BigDecimal> entry
                : state.getClosingPrices()
                .descendingMap().entrySet()) {

            if (count >= 10) {
                break;
            }

            result.append(entry.getKey())
                    .append(" : ")
                    .append(entry.getValue())
                    .append("\n");

            count++;
        }

        resultArea.setText(result.toString());
        resultArea.setCaretPosition(0);
    }
}
