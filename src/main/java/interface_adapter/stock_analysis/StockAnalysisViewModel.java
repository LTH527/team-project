package interface_adapter.stock_analysis;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Objects;

/**
 * View model for the stock analysis screen.
 */
public class StockAnalysisViewModel {

    public static final String STATE_PROPERTY = "state";

    private final PropertyChangeSupport support =
            new PropertyChangeSupport(this);

    private StockAnalysisState state =
            new StockAnalysisState();

    public StockAnalysisState getState() {
        return state;
    }

    public void setState(StockAnalysisState state) {
        StockAnalysisState oldState = this.state;
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
