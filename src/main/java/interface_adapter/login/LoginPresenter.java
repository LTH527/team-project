package interface_adapter.login;

import use_case.login.LoginOutputBoundary;
import use_case.login.LoginOutputData;

public class LoginPresenter implements LoginOutputBoundary {

    private final LoginViewModel loginViewModel;

    public LoginPresenter(LoginViewModel loginViewModel) {
        this.loginViewModel = loginViewModel;
    }

    @Override
    public void prepareSuccessView(LoginOutputData loginOutputData) {
        LoginState loginState = loginViewModel.getState();

        loginState.setUsername(loginOutputData.getUsername());
        loginState.setLoginError(null);

        loginViewModel.setState(loginState);
        loginViewModel.firePropertyChanged();
    }

    @Override
    public void prepareFailView(String errorMessage) {
        LoginState loginState = loginViewModel.getState();

        loginState.setLoginError(errorMessage);

        loginViewModel.setState(loginState);
        loginViewModel.firePropertyChanged();
    }
}
