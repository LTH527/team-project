package stockprototype.comparison.ui;

import stockprototype.StockTrade;
import stockprototype.comparison.ComparisonResult;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Predicate;

/** Daily closes and optional current-session trades indexed to the first shared close. */
final class IndexedPriceChart extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final MathContext PRECISION = MathContext.DECIMAL128;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BasicStroke SOLID = new BasicStroke(2.2f);
    private static final float[] DASH = {8, 5};
    private static final float DASH_PERIOD = DASH[0] + DASH[1];
    private static final ZoneId MARKET_ZONE = ZoneId.of("America/New_York");
    private static final DateTimeFormatter LIVE_DATE = DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.US);
    private ComparisonResult result;
    private LocalDate endDate;
    private String endLabel;
    private String firstLegend;
    private String secondLegend;
    private BigDecimal firstLiveIndex;
    private BigDecimal secondLiveIndex;

    IndexedPriceChart() {
        setOpaque(true);
        setBackground(ComparisonTheme.BACKGROUND);
        setForeground(ComparisonTheme.TEXT);
        setPreferredSize(new Dimension(580, 270));
        getAccessibleContext().setAccessibleName("Indexed stock performance");
    }

    void setResult(ComparisonResult result, boolean live) {
        setResult(result, live, LocalDate.now(MARKET_ZONE));
    }

    void setResult(ComparisonResult result, boolean live, LocalDate marketDate) {
        this.result = result;
        if (result == null) {
            getAccessibleContext().setAccessibleDescription("No comparison selected");
            repaint();
            return;
        }
        ComparisonResult.Day first = result.days.get(0);
        LocalDate lastCloseDate = result.days.get(result.days.size() - 1).date;
        boolean includeTrades = live && lastCloseDate.isBefore(marketDate);
        Predicate<StockTrade> isCurrent = trade -> includeTrades
                && trade.time.atZone(MARKET_ZONE).toLocalDate().equals(marketDate);
        StockTrade firstTrade = result.firstTrade().filter(isCurrent).orElse(null);
        StockTrade secondTrade = result.secondTrade().filter(isCurrent).orElse(null);
        firstLiveIndex = liveIndex(firstTrade, first.firstClose);
        secondLiveIndex = liveIndex(secondTrade, first.secondClose);
        boolean hasLivePoint = firstTrade != null || secondTrade != null;
        endDate = hasLivePoint ? marketDate : lastCloseDate;
        endLabel = hasLivePoint ? LIVE_DATE.format(endDate) + " · Live" : endDate.toString();
        firstLegend = legendText(result.firstSymbol, firstTrade);
        secondLegend = legendText(result.secondSymbol, secondTrade);
        String description = result.firstSymbol + " and " + result.secondSymbol
                + " indexed to 100 at the first common close. The first symbol is a solid line and "
                + "the second is dashed. Exact closes are in the Closing prices tab.";
        if (hasLivePoint) {
            description += " " + endLabel
                    + (firstTrade != null ? " " + firstLegend : "")
                    + (secondTrade != null ? " " + secondLegend : "");
        }
        getAccessibleContext().setAccessibleDescription(description);
        repaint();
    }

    private static BigDecimal liveIndex(StockTrade trade, BigDecimal firstClose) {
        return trade == null ? null : trade.price.multiply(HUNDRED).divide(firstClose, PRECISION);
    }

    private static String legendText(String symbol, StockTrade trade) {
        return trade == null ? symbol : symbol + " $" + trade.price.stripTrailingZeros().toPlainString();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(getForeground());
            if (result == null || result.days.size() < 2) {
                g.drawString("Compare two stocks to see their price histories.", 12, 26);
                return;
            }
            paintComparison(g);
        } finally {
            g.dispose();
        }
    }

    private void paintComparison(Graphics2D g) {
        FontMetrics font = g.getFontMetrics();
        int line = font.getHeight();
        int right = getWidth() - 14;
        int bottom = getHeight() - line - 12;
        int legendY = 8 + line;
        drawLegend(g, firstLegend, 12, legendY, SOLID);
        int secondX = 12 + 50 + font.stringWidth(firstLegend);
        if (secondX + 30 + font.stringWidth(secondLegend) > right) {
            secondX = 12;
            legendY += line + 2;
        }
        drawLegend(g, secondLegend, secondX, legendY, dashed(0));
        g.setColor(ComparisonTheme.MUTED);
        g.drawString("First close = 100. Green above 100, red below.", 12, legendY + line + 4);

        BigDecimal min = HUNDRED;
        BigDecimal max = HUNDRED;
        for (ComparisonResult.Day day : result.days) {
            min = min.min(day.firstIndex).min(day.secondIndex);
            max = max.max(day.firstIndex).max(day.secondIndex);
        }
        if (firstLiveIndex != null) {
            min = min.min(firstLiveIndex);
            max = max.max(firstLiveIndex);
        }
        if (secondLiveIndex != null) {
            min = min.min(secondLiveIndex);
            max = max.max(secondLiveIndex);
        }
        // A one-point margin keeps flat or nearly flat series on a readable scale.
        BigDecimal margin = max.subtract(min, PRECISION).movePointLeft(1).max(BigDecimal.ONE);
        BigDecimal low = min.subtract(margin, PRECISION).max(BigDecimal.ZERO);
        BigDecimal high = max.add(margin, PRECISION);
        BigDecimal range = high.subtract(low, PRECISION);
        int top = legendY + line * 2 + 4;
        int left = font.stringWidth("100");
        String[] ticks = new String[5];
        for (int i = 0; i < ticks.length; i++) {
            BigDecimal fraction = BigDecimal.valueOf(i * 25L, 2);
            ticks[i] = axisLabel(low.add(range.multiply(fraction, PRECISION), PRECISION));
            left = Math.max(left, font.stringWidth(ticks[i]));
        }
        left += 17;
        if (right - left < 50 || bottom - top < 36) {
            return;
        }
        double baseline = screenY(HUNDRED, low, range, top, bottom);
        int baselineTick = (int) Math.round(baseline);
        g.setStroke(new BasicStroke(1f));
        for (int i = 0; i < ticks.length; i++) {
            int y = bottom - Math.round((bottom - top) * i / 4f);
            if (Math.abs(y - baselineTick) >= line) {
                g.setColor(ComparisonTheme.GRID);
                g.drawLine(left, y, right, y);
                g.setColor(ComparisonTheme.MUTED);
                g.drawString(ticks[i], left - font.stringWidth(ticks[i]) - 6, y + font.getAscent() / 2);
            }
        }
        g.setColor(ComparisonTheme.MUTED);
        g.draw(new Line2D.Double(left, baseline, right, baseline));
        g.setColor(ComparisonTheme.TEXT);
        g.drawString("100", left - font.stringWidth("100") - 6, baselineTick + font.getAscent() / 2);
        g.setColor(ComparisonTheme.MUTED);
        LocalDate start = result.days.get(0).date;
        int dateY = bottom + line + 2;
        int endLabelX = right - font.stringWidth(endLabel);
        if (endLabelX > left + font.stringWidth(start.toString()) + 4) {
            g.drawString(start.toString(), left, dateY);
        }
        g.drawString(endLabel, Math.max(left, endLabelX), dateY);

        for (int series = 0; series < 2; series++) {
            double lastX = left;
            double lastY = baseline;
            double length = 0;
            boolean useDashes = series == 1;
            BigDecimal liveIndex = useDashes ? secondLiveIndex : firstLiveIndex;
            int closes = result.days.size();
            int points = closes + (liveIndex != null ? 1 : 0);
            for (int i = 1; i < points; i++) {
                LocalDate date = endDate;
                BigDecimal index;
                if (i == closes) {
                    index = liveIndex;
                } else {
                    ComparisonResult.Day day = result.days.get(i);
                    date = day.date;
                    index = useDashes ? day.secondIndex : day.firstIndex;
                }
                double elapsed = (double) (date.toEpochDay() - start.toEpochDay())
                        / (endDate.toEpochDay() - start.toEpochDay());
                double x = left + elapsed * (right - left);
                double y = screenY(index, low, range, top, bottom);
                // A segment that crosses the starting price changes color at the crossing.
                if ((lastY < baseline && y > baseline) || (lastY > baseline && y < baseline)) {
                    double crossX = lastX + (x - lastX) * (baseline - lastY) / (y - lastY);
                    drawSegment(g, lastX, lastY, crossX, baseline, baseline, useDashes, length);
                    length += Math.hypot(crossX - lastX, baseline - lastY);
                    lastX = crossX;
                    lastY = baseline;
                }
                drawSegment(g, lastX, lastY, x, y, baseline, useDashes, length);
                length += Math.hypot(x - lastX, y - lastY);
                lastX = x;
                lastY = y;
            }
            if (liveIndex != null) {
                g.setColor(ComparisonTheme.changeColor(liveIndex.compareTo(HUNDRED)));
                int markerY = (int) Math.round(lastY);
                if (useDashes) {
                    g.setStroke(new BasicStroke(2f));
                    g.drawRect(right - 5, markerY - 5, 10, 10);
                } else {
                    g.fillOval(right - 4, markerY - 4, 8, 8);
                }
            }
        }
    }

    private static void drawSegment(Graphics2D g, double x1, double y1, double x2, double y2,
                                    double baseline, boolean useDashes, double length) {
        double middle = (y1 + y2) / 2;
        if (middle < baseline) {
            g.setColor(ComparisonTheme.GAIN);
        } else if (middle > baseline) {
            g.setColor(ComparisonTheme.LOSS);
        } else {
            g.setColor(ComparisonTheme.TEXT);
        }
        g.setStroke(useDashes ? dashed(length) : SOLID);
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    private static BasicStroke dashed(double length) {
        return new BasicStroke(3.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                1, DASH, (float) (length % DASH_PERIOD));
    }

    private static double screenY(BigDecimal value, BigDecimal low, BigDecimal range, int top, int bottom) {
        double fraction = value.subtract(low, PRECISION).divide(range, PRECISION).doubleValue();
        return bottom - fraction * (bottom - top);
    }

    private static String axisLabel(BigDecimal value) {
        if (value.compareTo(BigDecimal.valueOf(1_000_000)) >= 0) {
            return new DecimalFormat("0.##E0", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(value);
        }
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private void drawLegend(Graphics2D g, String label, int x, int y, BasicStroke stroke) {
        g.setColor(ComparisonTheme.TEXT);
        g.setStroke(stroke);
        g.drawLine(x, y - 4, x + 22, y - 4);
        g.setColor(getForeground());
        g.drawString(label, x + 28, y);
    }
}
