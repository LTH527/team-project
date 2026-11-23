package app;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.sql.SQLException;

public final class PortfolioTestLauncher {

    private PortfolioTestLauncher() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                String username = "portfolio_test";

                JFrame frame = new JFrame("Portfolio Test");
                frame.setContentPane(
                        PortfolioAppBuilder.createPortfolioView(username));

                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(900, 650);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);

            } catch (SQLException | IllegalArgumentException exception) {
                JOptionPane.showMessageDialog(
                        null,
                        exception.getMessage(),
                        "Portfolio Launch Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
