package stockprototype.comparison.ui;

import static stockprototype.comparison.ui.ComparisonTheme.BACKGROUND;
import static stockprototype.comparison.ui.ComparisonTheme.LOSS;
import static stockprototype.comparison.ui.ComparisonTheme.MUTED;
import static stockprototype.comparison.ui.ComparisonTheme.TEXT;
import static stockprototype.comparison.ui.ComparisonTheme.button;
import static stockprototype.comparison.ui.ComparisonTheme.changeColor;
import static stockprototype.comparison.ui.ComparisonTheme.field;
import static stockprototype.comparison.ui.ComparisonTheme.label;
import static stockprototype.comparison.ui.ComparisonTheme.panel;
import static stockprototype.comparison.ui.ComparisonTheme.period;
import static stockprototype.comparison.ui.ComparisonTheme.table;
import static stockprototype.comparison.ui.ComparisonTheme.tabs;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.HierarchyEvent;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import stockprototype.StockSymbolDirectory;
import stockprototype.comparison.CompareStocks;
import stockprototype.comparison.ComparisonResult;

public final class ComparisonPanel extends JPanel implements ComparisonPresenter.View {
    private static final long serialVersionUID = 1L;
    private enum Window {
        SIXTY("60 trading days (about 3 months)", 60),
        TWENTY("20 trading days (about 1 month)", 20),
        ALL("All available", 0);

        private final String label;
        private final int limit;

        Window(String label, int limit) {
            this.label = label;
            this.limit = limit;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final TickerField firstSymbol = new TickerField("IBM", this::requestSymbols);
    private final TickerField secondSymbol = new TickerField("AAPL", this::requestSymbols);
    private final JComboBox<Window> window = new JComboBox<>(Window.values());
    private final JButton compare = new JButton("Compare");
    private final JLabel status = label("Select two symbols and click Compare.", MUTED);
    private final CardLayout cards = new CardLayout();
    private final JPanel body = panel(cards);
    private final JLabel firstTrade = label("", TEXT);
    private final JLabel secondTrade = label("", TEXT);
    private final JLabel period = label("", TEXT);
    private final JLabel performance = label("", TEXT);
    private final JLabel correlation = label("", TEXT);
    private final JTable summary = new JTable();
    private final JTable history = new JTable();
    private final IndexedPriceChart chart = new IndexedPriceChart();
    private final ComparisonController controller;
    private final boolean live;
    private final StockSymbolDirectory symbolDirectory;
    private SwingWorker<TickerSearch, Void> symbolLoader;

    public ComparisonPanel(CompareStocks useCase, boolean live, StockSymbolDirectory symbolDirectory) {
        super(new BorderLayout(0, 10));
        this.live = live;
        this.symbolDirectory = symbolDirectory;
        controller = new ComparisonController(useCase, new ComparisonPresenter(this));
        setBackground(BACKGROUND);
        setForeground(TEXT);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));
        field(firstSymbol);
        field(secondSymbol);
        button(compare);
        period(window);
        table(summary);
        table(history);
        setPreferredSize(new Dimension(840, 630));
        add(buildForm(live), BorderLayout.NORTH);
        body.add(panel(new BorderLayout()), "empty");
        body.add(buildResults(), "results");
        add(body, BorderLayout.CENTER);
        compare.addActionListener(event -> submit());
        firstSymbol.addActionListener(event -> submit());
        secondSymbol.addActionListener(event -> submit());
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                requestSymbols();
            }
        });
    }

    private void requestSymbols() {
        if (symbolLoader != null) {
            return;
        }
        symbolLoader = new SwingWorker<>() {
            @Override
            protected TickerSearch doInBackground() throws IOException {
                return new TickerSearch(symbolDirectory.loadListings(cached -> {
                    TickerSearch saved = new TickerSearch(cached);
                    SwingUtilities.invokeLater(() -> showSuggestions(saved));
                }));
            }

            @Override
            protected void done() {
                try {
                    showSuggestions(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    catalogUnavailable();
                } catch (ExecutionException exception) {
                    catalogUnavailable();
                }
            }
        };
        symbolLoader.execute();
    }

    private void showSuggestions(TickerSearch search) {
        firstSymbol.setSearch(search);
        secondSymbol.setSearch(search);
    }

    private void catalogUnavailable() {
        symbolLoader = null;
        firstSymbol.catalogUnavailable();
        secondSymbol.catalogUnavailable();
    }

    private JPanel buildForm(boolean live) {
        JPanel form = panel(new BorderLayout(0, 6));
        JLabel title = label("Compare two stocks", TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        JLabel source = label(live ? "Alpaca · IEX exchange data"
                : "Synthetic demo data · IBM, AAPL, MSFT, NVDA and TSLA", MUTED);
        JPanel heading = panel(null);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(title);
        heading.add(source);
        form.add(heading, BorderLayout.NORTH);

        JPanel fields = panel(new FlowLayout(FlowLayout.LEADING, 6, 6));
        fields.add(labelFor("Stock 1", firstSymbol));
        fields.add(firstSymbol);
        fields.add(labelFor("Stock 2", secondSymbol));
        fields.add(secondSymbol);
        fields.add(labelFor("Period", window));
        fields.add(window);
        fields.add(compare);
        form.add(fields, BorderLayout.CENTER);
        form.add(status, BorderLayout.SOUTH);
        return form;
    }

    private static JLabel labelFor(String label, JComponent field) {
        JLabel component = label(label, MUTED);
        component.setLabelFor(field);
        return component;
    }

    private JPanel buildResults() {
        JPanel results = panel(new BorderLayout(0, 8));
        JPanel overview = panel(new BorderLayout(0, 6));
        JPanel prices = panel(null);
        prices.setLayout(new BoxLayout(prices, BoxLayout.Y_AXIS));
        prices.add(label(live ? "Latest IEX trades" : "Latest synthetic trades", MUTED));
        prices.add(firstTrade);
        prices.add(secondTrade);
        overview.add(prices, BorderLayout.NORTH);
        JPanel summaryBlock = panel(new BorderLayout());
        summary.setFillsViewportHeight(true);
        summary.setRowSelectionAllowed(false);
        summary.getTableHeader().setReorderingAllowed(false);
        summary.getAccessibleContext().setAccessibleName("Comparison summary");
        summaryBlock.add(summary.getTableHeader(), BorderLayout.NORTH);
        summaryBlock.add(summary, BorderLayout.CENTER);
        overview.add(summaryBlock, BorderLayout.CENTER);
        JPanel details = panel(null);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.add(performance);
        details.add(correlation);
        details.add(label("Estimate: one more interval at the period’s compound growth rate.", MUTED));
        overview.add(details, BorderLayout.SOUTH);
        results.add(overview, BorderLayout.NORTH);

        JTabbedPane data = new JTabbedPane();
        tabs(data);
        data.addTab("Performance chart", chart);
        history.getTableHeader().setReorderingAllowed(false);
        history.getAccessibleContext().setAccessibleName("Shared daily closing prices");
        history.setFillsViewportHeight(true);
        JScrollPane historyScroll = new JScrollPane(history);
        historyScroll.setBorder(BorderFactory.createEmptyBorder());
        historyScroll.getViewport().setBackground(BACKGROUND);
        historyScroll.setBackground(BACKGROUND);
        data.addTab("Closing prices", historyScroll);
        JPanel chartAndHistory = panel(new BorderLayout(0, 4));
        period.getAccessibleContext().setAccessibleName("Comparison date range");
        chartAndHistory.add(period, BorderLayout.NORTH);
        chartAndHistory.add(data, BorderLayout.CENTER);
        results.add(chartAndHistory, BorderLayout.CENTER);
        results.add(label("Raw closes in each stock’s quote currency. "
                + "Price changes exclude dividend and split adjustments.", MUTED), BorderLayout.SOUTH);
        return results;
    }

    private void submit() {
        Window selected = (Window) window.getSelectedItem();
        if (selected == null) {
            showError("Select a comparison period.");
            return;
        }
        controller.compare(firstSymbol.getText(), secondSymbol.getText(), selected.limit);
    }

    @Override
    public void startLoading() {
        setInputsEnabled(false);
        status.setForeground(MUTED);
        status.setText(live ? "Loading prices from Alpaca…"
                : "Loading sample prices…");
        cards.show(body, "empty");
    }

    @Override
    public void showError(String message) {
        status.setForeground(LOSS);
        status.setText("Comparison failed: " + message);
        cards.show(body, "empty");
        setInputsEnabled(true);
    }

    @Override
    public void showComparison(ComparisonResult result, String dateRange, String leader,
                               String returnCorrelation, String firstTradeText, String secondTradeText,
                               TableModel summaryData, TableModel historyData) {
        status.setForeground(MUTED);
        status.setText("Comparison of " + result.firstSymbol + " and " + result.secondSymbol);
        firstTrade.setText(firstTradeText);
        secondTrade.setText(secondTradeText);
        period.setText("Daily closes · " + dateRange);
        performance.setText(leader);
        correlation.setText(returnCorrelation);
        summary.setModel(summaryData);
        final Color[] changes = {changeColor(result.firstChange.signum()), changeColor(result.secondChange.signum())};
        summary.getColumn("Price change").setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                           boolean focused, int row, int column) {
                super.getTableCellRendererComponent(table, value, selected, focused, row, column);
                setForeground(changes[table.convertRowIndexToModel(row)]);
                setFont(table.getFont().deriveFont(Font.BOLD));
                setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                return this;
            }
        });
        history.setModel(historyData);
        chart.setResult(result, live);
        cards.show(body, "results");
        setInputsEnabled(true);
    }

    private void setInputsEnabled(boolean enabled) {
        firstSymbol.setEnabled(enabled);
        secondSymbol.setEnabled(enabled);
        window.setEnabled(enabled);
        compare.setEnabled(enabled);
    }
}
