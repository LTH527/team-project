
package app;

import data_access.CourseUserDataAccessObject;
import entity.CommonUserFactory;
import entity.UserFactory;
import view.LoginView;
import view.SignupView;

import javax.swing.*;
import java.awt.*;

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            // Create one shared DAO
            UserFactory userFactory = new CommonUserFactory();

            CourseUserDataAccessObject userDAO =
                    new CourseUserDataAccessObject(userFactory);

            // Build Signup and Login views
            SignupView signupView =
                    SignupAppBuilder.createSignupView(
                            userDAO, userFactory);

            LoginView loginView =
                    LoginAppBuilder.createLoginView(userDAO);

            // Create main window
            JFrame frame =
                    new JFrame("Stock Analysis Platform");

            // Use CardLayout to switch between views
            JPanel mainPanel = new JPanel(new CardLayout());

            mainPanel.add(signupView, "signup");
            mainPanel.add(loginView, "login");

            CardLayout cardLayout =
                    (CardLayout) mainPanel.getLayout();

            // Navigation buttons
            JButton signupButton =
                    new JButton("Sign Up");

            JButton loginButton =
                    new JButton("Log In");

            signupButton.addActionListener(e ->
                    cardLayout.show(mainPanel, "signup"));

            loginButton.addActionListener(e ->
                    cardLayout.show(mainPanel, "login"));

            JPanel navigationPanel = new JPanel();
            navigationPanel.add(signupButton);
            navigationPanel.add(loginButton);

            // Combine navigation and views
            JPanel contentPanel = new JPanel(
                    new BorderLayout());

            contentPanel.add(
                    navigationPanel, BorderLayout.NORTH);

            contentPanel.add(
                    mainPanel, BorderLayout.CENTER);

            frame.setContentPane(contentPanel);

            frame.setDefaultCloseOperation(
                    JFrame.EXIT_ON_CLOSE);

            frame.setSize(500, 400);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
