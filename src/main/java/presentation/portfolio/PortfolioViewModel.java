package presentation.portfolio;

import use_case.portfolio.HoldingOutputData;
import use_case.portfolio.TransactionOutputData;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Stores the presentation state of the simulated portfolio.
 */
public final class PortfolioViewModel {

    private final PropertyChangeSupport support =
            new PropertyChangeSupport(this);

    private String message = "";
    private boolean success;
    private BigDecimal availableCash = BigDecimal.ZERO;
    private BigDecimal portfolioValue = BigDecimal.ZERO;
    private BigDecimal profitLoss = BigDecimal.ZERO;

    private List<HoldingOutputData> holdings = new ArrayList<>();
    private List<TransactionOutputData> transactions = new ArrayList<>();

    public synchronized String getMessage() {
        return message;
    }

    public synchronized boolean isSuccess() {
        return success;
    }

    public synchronized BigDecimal getAvailableCash() {
        return availableCash;
    }

    public synchronized BigDecimal getPortfolioValue() {
        return portfolioValue;
    }

    public synchronized BigDecimal getProfitLoss() {
        return profitLoss;
    }

    public synchronized List<HoldingOutputData> getHoldings() {
        return Collections.unmodifiableList(new ArrayList<>(holdings));
    }

    public synchronized List<TransactionOutputData> getTransactions() {
        return Collections.unmodifiableList(new ArrayList<>(transactions));
    }

    public synchronized void setTradeResult(
            String message,
            boolean success,
            BigDecimal availableCash) {
        this.message = Objects.requireNonNull(message, "message must not be null");
        this.success = success;
        this.availableCash = Objects.requireNonNull(
                availableCash, "availableCash must not be null");
        firePropertyChanged();
    }

    public synchronized void setPortfolioData(
            BigDecimal availableCash,
            BigDecimal portfolioValue,
            BigDecimal profitLoss,
            List<HoldingOutputData> holdings) {

        this.availableCash = Objects.requireNonNull(
                availableCash, "availableCash must not be null");
        this.portfolioValue = Objects.requireNonNull(
                portfolioValue, "portfolioValue must not be null");
        this.profitLoss = Objects.requireNonNull(
                profitLoss, "profitLoss must not be null");
        Objects.requireNonNull(holdings, "holdings must not be null");
        if (holdings.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("holdings must not contain null");
        }
        this.holdings = new ArrayList<>(holdings);
        firePropertyChanged();
    }

    public synchronized void setTransactions(
            List<TransactionOutputData> transactions) {
        Objects.requireNonNull(transactions, "transactions must not be null");
        if (transactions.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("transactions must not contain null");
        }
        this.transactions = new ArrayList<>(transactions);
        firePropertyChanged();
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(
                Objects.requireNonNull(listener, "listener must not be null"));
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(
                Objects.requireNonNull(listener, "listener must not be null"));
    }

    private void firePropertyChanged() {
        support.firePropertyChange("state", null, this);
    }
}
