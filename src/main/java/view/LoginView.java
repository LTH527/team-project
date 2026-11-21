package view;

import interface_adapter.login.LoginController;
import interface_adapter.login.LoginState;
import interface_adapter.login.LoginViewModel;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

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
            if (loginController != null) {
                String username =
                        usernameInputField.getText();

                String password = new String(
                        passwordInputField.getPassword());

                loginController.execute(username, password);
            }
        });
    }

    public void setLoginController(
            LoginController loginController) {
        this.loginController = loginController;
    }


    @Override
    public void propertyChange(PropertyChangeEvent evt) {
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
