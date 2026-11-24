package stockprototype.comparison.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import stockprototype.StockListing;

/** A ticker can be typed directly or found by searching its symbol or company name. */
final class TickerField extends JTextField {
    private static final long serialVersionUID = 1L;
    private static final int VISIBLE_ROWS = 8;
    private final Runnable requestSymbols;
    private final JPopupMenu popup = new JPopupMenu();
    private final JList<StockListing> matches = new JList<>();
    private final JScrollPane scroll = new JScrollPane(matches);
    private final JLabel note = ComparisonTheme.label("", ComparisonTheme.MUTED);
    private TickerSearch search;
    private boolean failed;
    private boolean choosing;
    private String prefix = "";

    TickerField(String initial, Runnable requestSymbols) {
        super(initial, 8);
        this.requestSymbols = requestSymbols;
        setToolTipText("Search by stock symbol or company name");
        getAccessibleContext().setAccessibleDescription(getToolTipText());
        popup.setFocusable(false);
        popup.setBorder(BorderFactory.createLineBorder(ComparisonTheme.GRID));
        popup.setBackground(ComparisonTheme.SURFACE);
        note.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        matches.setFocusable(false);
        matches.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        matches.setBackground(ComparisonTheme.SURFACE);
        matches.setForeground(ComparisonTheme.TEXT);
        matches.setSelectionBackground(ComparisonTheme.GRID);
        matches.setSelectionForeground(ComparisonTheme.TEXT);
        matches.setFixedCellWidth(360);
        matches.setFixedCellHeight(getFontMetrics(getFont()).getHeight() + 6);
        matches.getAccessibleContext().setAccessibleName("Stock symbol suggestions");
        matches.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int row,
                                                          boolean selected, boolean focused) {
                StockListing listing = (StockListing) value;
                String label = listing.name.isEmpty() ? listing.symbol : listing.symbol + "   " + listing.name;
                return super.getListCellRendererComponent(list, label, row, selected, focused);
            }
        });
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setFocusable(false);
        scroll.getVerticalScrollBar().setFocusable(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(ComparisonTheme.SURFACE);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event) && isEnabled() && !popup.isVisible()) {
                    selectAll();
                    open("");
                }
            }
        });
        matches.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                int index = matches.locationToIndex(event.getPoint());
                Rectangle cell = index < 0 ? null : matches.getCellBounds(index, index);
                if (SwingUtilities.isLeftMouseButton(event) && cell != null && cell.contains(event.getPoint())) {
                    choose(matches.getModel().getElementAt(index).symbol);
                }
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                popup.setVisible(false);
            }
        });
        getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { edited(); }
            @Override public void removeUpdate(DocumentEvent event) { edited(); }
            @Override public void changedUpdate(DocumentEvent event) { edited(); }
        });
    }

    void setSearch(TickerSearch available) {
        search = available;
        failed = false;
        if (popup.isVisible()) {
            refresh();
        }
    }

    void catalogUnavailable() {
        failed = true;
        if (popup.isVisible()) {
            refresh();
        }
    }

    private void edited() {
        if (!choosing && isFocusOwner() && isEnabled() && isShowing()) {
            open(getText().strip());
        }
    }

    private void open(String filter) {
        prefix = filter;
        boolean opening = !popup.isVisible();
        if (opening) {
            failed = false;
        }
        refresh();
        if (opening) {
            popup.show(this, 0, getHeight());
            if (search == null) {
                requestSymbols.run();
            }
        }
    }

    private void refresh() {
        List<StockListing> rows = search == null ? List.of() : search.find(prefix);
        matches.setModel(new AbstractListModel<>() {
            @Override public int getSize() { return rows.size(); }
            @Override public StockListing getElementAt(int index) { return rows.get(index); }
        });
        if (!rows.isEmpty()) {
            // A name-only search can be accepted with Enter; exact ticker input still submits normally.
            String firstSymbol = rows.get(0).symbol;
            if (!firstSymbol.regionMatches(true, 0, prefix, 0, prefix.length())) {
                matches.setSelectedIndex(0);
            }
            matches.ensureIndexIsVisible(0);
        } else {
            String message = failed ? "List unavailable. Type a symbol." : "Loading symbols…";
            note.setText(search == null ? message : "No matching symbols or companies");
        }
        JComponent content = rows.isEmpty() ? note : scroll;
        if (popup.getComponentCount() == 0 || popup.getComponent(0) != content) {
            popup.removeAll();
            popup.add(content);
        }
        int height = Math.min(VISIBLE_ROWS, Math.max(1, rows.size())) * matches.getFixedCellHeight() + 2;
        Dimension size = new Dimension(Math.max(380, getWidth()), height);
        if (!size.equals(popup.getPreferredSize())) {
            popup.setPopupSize(size);
        }
        popup.revalidate();
        popup.repaint();
    }

    @Override
    protected boolean processKeyBinding(KeyStroke key, KeyEvent event, int condition, boolean pressed) {
        if (pressed && condition == WHEN_FOCUSED && event.getModifiersEx() == 0 && isEnabled()) {
            int code = key.getKeyCode();
            if ((code == KeyEvent.VK_DOWN || code == KeyEvent.VK_UP) && isShowing()) {
                if (!popup.isVisible()) {
                    open("");
                }
                int size = matches.getModel().getSize();
                if (size > 0) {
                    int selected = matches.getSelectedIndex();
                    int next = code == KeyEvent.VK_DOWN ? Math.min(selected + 1, size - 1)
                            : (selected < 0 ? size - 1 : Math.max(0, selected - 1));
                    matches.setSelectedIndex(next);
                    matches.ensureIndexIsVisible(next);
                }
                return true;
            }
            if (popup.isVisible() && code == KeyEvent.VK_ESCAPE) {
                popup.setVisible(false);
                return true;
            }
            if (popup.isVisible() && code == KeyEvent.VK_ENTER) {
                StockListing selected = matches.getSelectedValue();
                if (selected != null) {
                    choose(selected.symbol);
                    return true;
                }
                popup.setVisible(false);
            }
        }
        return super.processKeyBinding(key, event, condition, pressed);
    }

    private void choose(String symbol) {
        choosing = true;
        try {
            setText(symbol);
            setCaretPosition(symbol.length());
        } finally {
            choosing = false;
            popup.setVisible(false);
        }
    }
}
