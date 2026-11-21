package app;

import data_access.CourseUserDataAccessObject;
import entity.UserFactory;
import interface_adapter.signup.SignupController;
import interface_adapter.signup.SignupPresenter;
import interface_adapter.signup.SignupViewModel;
import use_case.signup.SignupInteractor;
import use_case.signup.SignupOutputBoundary;
import view.SignupView;

public class SignupAppBuilder {

    public static SignupView createSignupView(
            CourseUserDataAccessObject userDAO,
            UserFactory userFactory) {

        SignupViewModel viewModel = new SignupViewModel();

        SignupOutputBoundary presenter =
                new SignupPresenter(viewModel);

        SignupInteractor interactor =
                new SignupInteractor(userDAO, presenter, userFactory);

        SignupController controller =
                new SignupController(interactor);

        return new SignupView(controller, viewModel);
    }
}
