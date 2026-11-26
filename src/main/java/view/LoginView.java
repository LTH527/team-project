package view;

import interface_adapter.login.LoginController;
import interface_adapter.login.LoginState;
import interface_adapter.login.LoginViewModel;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.ExecutionException;

public class LoginView extends JPanel
        implements PropertyChangeListener {

    private final LoginViewModel loginViewModel;

    private LoginController loginController;

    private final JTextField usernameInputField =
            new JTextField(15);

    private final JPasswordField passwordInputField =
            new JPasswordField(15);

    private final JButton loginButton =
            new JButton("Log In");

    private final JLabel errorLabel =
            new JLabel("");

    public LoginView(LoginViewModel loginViewModel) {
        this.loginViewModel = loginViewModel;
        this.loginViewModel.addPropertyChangeListener(this);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Log In");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel usernamePanel = new JPanel();
        usernamePanel.add(new JLabel("Username:"));
        usernamePanel.add(usernameInputField);

        JPanel passwordPanel = new JPanel();
        passwordPanel.add(new JLabel("Password:"));
        passwordPanel.add(passwordInputField);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(loginButton);

        errorLabel.setForeground(Color.RED);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        add(title);
        add(usernamePanel);
        add(passwordPanel);
        add(buttonPanel);
        add(errorLabel);

        loginButton.addActionListener(event -> {
            if (loginController == null || !loginButton.isEnabled()) {
                return;
            }

            String username = usernameInputField.getText();
            String password = new String(
                    passwordInputField.getPassword());

            loginButton.setEnabled(false);
            errorLabel.setForeground(Color.GRAY);
            errorLabel.setText("Logging in...");

            SwingWorker<Void, Void> worker = new SwingWorker<>() {

                @Override
                protected Void doInBackground() {
                    loginController.execute(username, password);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        errorLabel.setForeground(Color.RED);
                        errorLabel.setText("Login was interrupted.");
                    }
                    catch (ExecutionException e) {
                        errorLabel.setForeground(Color.RED);
                        errorLabel.setText("An unexpected login error occurred.");
                    }
                    catch (java.util.concurrent.CancellationException e) {
                        errorLabel.setForeground(Color.RED);
                        errorLabel.setText("Login was cancelled.");
                    }
                    finally {
                        loginButton.setEnabled(true);
                    }
                }
            };

            worker.execute();
        });
    }

    public void setLoginController(
            LoginController loginController) {
        this.loginController = loginController;
    }


    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (SwingUtilities.isEventDispatchThread()) {
            updateLoginView();
        }
        else {
            SwingUtilities.invokeLater(this::updateLoginView);
        }
    }

    private void updateLoginView() {
        LoginState state = loginViewModel.getState();

        if (state.getLoginError() != null) {
            errorLabel.setForeground(Color.RED);
            errorLabel.setText(state.getLoginError());
        }
        else {
            errorLabel.setForeground(new Color(0, 128, 0));
            errorLabel.setText(
                    "Login successful! Welcome, "
                            + state.getUsername() + "!");
        }
    }
}
