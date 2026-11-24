package stockprototype.comparison.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import stockprototype.StockComparisonApp;

class ComparisonPeriodTest {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.US);
    private ComparisonPanel panel;
    private JComboBox<?> periods;

    @Test
    void defaultAndShorterPeriodShowTheDatesActuallyCompared() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            panel = StockComparisonApp.createPanel(new String[]{"--demo"});
            JLabel periodLabel = namedComponent(JLabel.class, "Period");
            periods = (JComboBox<?>) periodLabel.getLabelFor();
            assertEquals("60 trading days (about 3 months)", String.valueOf(periods.getSelectedItem()));
        });
        compareAndCheckDates(60);

        SwingUtilities.invokeAndWait(() -> {
            Object shorter = IntStream.range(0, periods.getItemCount())
                    .mapToObj(periods::getItemAt)
                    .filter(item -> "20 trading days (about 1 month)".equals(String.valueOf(item)))
                    .findFirst().orElseThrow();
            periods.setSelectedItem(shorter);
        });
        compareAndCheckDates(20);
    }

    private void compareAndCheckDates(int expectedCount) throws Exception {
        CountDownLatch completed = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            JButton compare = namedComponent(JButton.class, "Compare");
            compare.addPropertyChangeListener("enabled", event -> {
                if (Boolean.TRUE.equals(event.getNewValue())) {
                    completed.countDown();
                }
            });
            compare.doClick(0);
        });
        assertTrue(completed.await(5, TimeUnit.SECONDS), "Offline comparison timed out");
        SwingUtilities.invokeAndWait(() -> {
            JTable history = namedComponent(JTable.class, "Shared daily closing prices");
            assertEquals(expectedCount, history.getRowCount());
            LocalDate first = (LocalDate) history.getValueAt(0, 0);
            LocalDate last = (LocalDate) history.getValueAt(expectedCount - 1, 0);
            String range = namedComponent(JLabel.class, "Comparison date range").getText();
            assertTrue(range.contains(DATE.format(first) + " to " + DATE.format(last)), range);
            assertTrue(range.contains(expectedCount + " shared trading days"), range);
        });
    }

    private <T extends Component> T namedComponent(Class<T> type, String name) {
        return descendants(panel).filter(type::isInstance).map(type::cast)
                .filter(component -> name.equals(component.getAccessibleContext().getAccessibleName()))
                .findFirst().orElseThrow(() -> new AssertionError("Missing " + name));
    }

    private static Stream<Component> descendants(Component component) {
        Stream<Component> self = Stream.of(component);
        if (component instanceof Container) {
            return Stream.concat(self, Arrays.stream(((Container) component).getComponents())
                    .flatMap(ComparisonPeriodTest::descendants));
        }
        return self;
    }
}
