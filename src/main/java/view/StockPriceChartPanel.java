package view;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Displays historical stock closing prices as a line chart.
 */
public class StockPriceChartPanel extends JPanel {

    private NavigableMap<LocalDate, BigDecimal> prices =
            new TreeMap<>();

    public StockPriceChartPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(750, 320));
        setBorder(BorderFactory.createTitledBorder(
                "Historical Closing Prices"));
    }

    public void setClosingPrices(
            NavigableMap<LocalDate, BigDecimal> newPrices) {
        this.prices = new TreeMap<>(newPrices);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g = (Graphics2D) graphics.create();

        try {
            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            if (prices.isEmpty()) {
                g.setColor(Color.GRAY);
                g.drawString("No historical prices to display.",
                        30, getHeight() / 2);
                return;
            }

            List<Map.Entry<LocalDate, BigDecimal>> data =
                    new ArrayList<>(prices.entrySet());

            int left = 75;
            int right = getWidth() - 30;
            int top = 35;
            int bottom = getHeight() - 50;

            int width = right - left;
            int height = bottom - top;

            if (width <= 0 || height <= 0) {
                return;
            }

            double minPrice = Double.MAX_VALUE;
            double maxPrice = -Double.MAX_VALUE;

            for (Map.Entry<LocalDate, BigDecimal> entry : data) {
                double price = entry.getValue().doubleValue();
                minPrice = Math.min(minPrice, price);
                maxPrice = Math.max(maxPrice, price);
            }

            double range = maxPrice - minPrice;
            double padding = Math.max(range * 0.1, 0.01);

            minPrice -= padding;
            maxPrice += padding;

            // Draw the price grid and Y-axis labels.
            g.setColor(Color.LIGHT_GRAY);

            for (int i = 0; i <= 4; i++) {
                int y = top + height * i / 4;

                g.drawLine(left, y, right, y);

                double value = maxPrice
                        - (maxPrice - minPrice) * i / 4.0;

                g.setColor(Color.DARK_GRAY);
                g.drawString(String.format("%.2f", value),
                        10, y + 5);
                g.setColor(Color.LIGHT_GRAY);
            }

            // Draw the historical price line.
            Path2D path = new Path2D.Double();

            for (int i = 0; i < data.size(); i++) {
                double price = data.get(i)
                        .getValue().doubleValue();

                double x = data.size() == 1
                        ? left + width / 2.0
                        : left + (double) i * width
                        / (data.size() - 1);

                double y = top
                        + (maxPrice - price)
                        / (maxPrice - minPrice) * height;

                if (i == 0) {
                    path.moveTo(x, y);
                }
                else {
                    path.lineTo(x, y);
                }

                if (data.size() == 1) {
                    g.setColor(new Color(40, 110, 190));
                    g.fillOval((int) x - 4, (int) y - 4, 8, 8);
                }
            }

            g.setColor(new Color(40, 110, 190));
            g.setStroke(new BasicStroke(2.5f));
            g.draw(path);

            // Draw starting and ending dates.
            g.setColor(Color.DARK_GRAY);

            String startDate =
                    data.get(0).getKey().toString();
            String endDate =
                    data.get(data.size() - 1).getKey().toString();

            g.drawString(startDate, left, bottom + 25);

            int endWidth = g.getFontMetrics()
                    .stringWidth(endDate);

            g.drawString(endDate,
                    right - endWidth, bottom + 25);
        }
        finally {
            g.dispose();
        }
    }
}
