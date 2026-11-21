package app;

import data_access.CourseUserDataAccessObject;
import interface_adapter.login.LoginController;
import interface_adapter.login.LoginPresenter;
import interface_adapter.login.LoginViewModel;
import use_case.login.LoginInteractor;
import use_case.login.LoginOutputBoundary;
import view.LoginView;

public class LoginAppBuilder {

    public static LoginView createLoginView(
            CourseUserDataAccessObject userDAO) {

        LoginViewModel viewModel = new LoginViewModel();

        LoginOutputBoundary presenter =
                new LoginPresenter(viewModel);

        LoginInteractor interactor =
                new LoginInteractor(userDAO, presenter);

        LoginController controller =
                new LoginController(interactor);

        LoginView loginView = new LoginView(viewModel);
        loginView.setLoginController(controller);

        return loginView;
    }
}
