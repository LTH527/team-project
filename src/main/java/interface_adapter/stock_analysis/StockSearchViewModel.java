package interface_adapter.stock_analysis;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Objects;

/**
 * ViewModel for stock search suggestions.
 */
public class StockSearchViewModel {

    public static final String STATE_PROPERTY = "state";

    private final PropertyChangeSupport support =
            new PropertyChangeSupport(this);

    private StockSearchState state = new StockSearchState();

    public StockSearchState getState() {
        return state;
    }

    public void setState(StockSearchState state) {
        StockSearchState oldState = this.state;
        this.state = Objects.requireNonNull(state);

        support.firePropertyChange(
                STATE_PROPERTY, oldState, this.state);
    }

    public void addPropertyChangeListener(
            PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(
            PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }
}
