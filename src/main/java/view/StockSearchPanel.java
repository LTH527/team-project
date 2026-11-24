package view;

import entity.StockSearchResult;
import interface_adapter.stock_analysis.StockSearchController;
import interface_adapter.stock_analysis.StockSearchState;
import interface_adapter.stock_analysis.StockSearchViewModel;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Search bar with dynamic stock suggestions.
 */
public class StockSearchPanel extends JPanel {

    private final StockSearchController controller;
    private final StockSearchViewModel viewModel;
    private final Consumer<String> onSearch;
    private final Consumer<String> onError;

    private final JTextField searchField = new JTextField("AAPL");
    private final JButton searchButton = new JButton("Search");
    private final JPopupMenu dropdown = new JPopupMenu();

    private boolean selecting = false;

    public StockSearchPanel(
            StockSearchController controller,
            StockSearchViewModel viewModel,
            Consumer<String> onSearch,
            Consumer<String> onError) {

        this.controller = Objects.requireNonNull(controller);
        this.viewModel = Objects.requireNonNull(viewModel);
        this.onSearch = Objects.requireNonNull(onSearch);
        this.onError = Objects.requireNonNull(onError);

        dropdown.setFocusable(false);
        dropdown.setRequestFocusEnabled(false);

        setLayout(new BorderLayout(10, 0));

        add(searchField, BorderLayout.CENTER);
        add(searchButton, BorderLayout.EAST);

        // Update suggestions when search results change.
        viewModel.addPropertyChangeListener(event -> {
            if (StockSearchViewModel.STATE_PROPERTY.equals(
                    event.getPropertyName())) {

                StockSearchState state =
                        (StockSearchState) event.getNewValue();

                SwingUtilities.invokeLater(
                        () -> updateDropdown(state.getResults()));
            }
        });

        // Listen for changes in the search field.
        searchField.getDocument().addDocumentListener(
                new DocumentListener() {

                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        updateSearch();
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        updateSearch();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        updateSearch();
                    }
                });

        searchButton.addActionListener(e -> submitSearch());
        searchField.addActionListener(e -> submitSearch());
    }

    private void updateSearch() {
        if (selecting) {
            return;
        }

        String keywords = searchField.getText().trim();

        if (keywords.isEmpty()) {
            dropdown.setVisible(false);
            return;
        }

        controller.execute(keywords);
    }

    private void updateDropdown(List<StockSearchResult> results) {

        // Do not close and reopen the dropdown on every keystroke.
        dropdown.removeAll();

        if (!searchField.isShowing()) {
            dropdown.setVisible(false);
            return;
        }

        int limit = Math.min(results.size(), 6);

        for (int i = 0; i < limit; i++) {
            StockSearchResult stock = results.get(i);

            JMenuItem item = new JMenuItem(
                    stock.getSymbol() + " - "
                            + stock.getCompanyName());

            item.setFocusable(false);
            item.setRequestFocusEnabled(false);

            item.addActionListener(e -> selectStock(stock));
            dropdown.add(item);
        }

        if (dropdown.getComponentCount() == 0) {
            dropdown.setVisible(false);
            return;
        }

        if (!dropdown.isVisible()) {
            dropdown.show(
                    searchField, 0, searchField.getHeight());
        }
        else {
            dropdown.revalidate();
            dropdown.repaint();
        }

        // Keep keyboard focus in the search field.
        SwingUtilities.invokeLater(
                searchField::requestFocusInWindow);
    }

    private void selectStock(StockSearchResult stock) {
        selecting = true;

        try {
            searchField.setText(stock.getSymbol());
        }
        finally {
            selecting = false;
        }

        dropdown.setVisible(false);
    }

    private void submitSearch() {
        dropdown.setVisible(false);

        String input = searchField.getText().trim();

        if (input.isEmpty()) {
            onError.accept("Please enter a stock.");
            return;
        }

        controller.execute(input);

        StockSearchState state = viewModel.getState();

        for (StockSearchResult stock : state.getResults()) {
            if (stock.getSymbol().equalsIgnoreCase(input)
                    || stock.getCompanyName().equalsIgnoreCase(input)) {

                onSearch.accept(stock.getSymbol());
                return;
            }
        }

        if (!state.getErrorMessage().isEmpty()) {
            onError.accept(state.getErrorMessage());
        }
        else {
            onError.accept(
                    "Stock not found in supported stock list.");
        }
    }

    public void setLoading(boolean loading) {
        searchButton.setEnabled(!loading);
        searchField.setEnabled(!loading);

        if (loading) {
            dropdown.setVisible(false);
        }
    }
}
