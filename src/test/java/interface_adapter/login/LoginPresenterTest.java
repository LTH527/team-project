package interface_adapter.login;

import org.junit.jupiter.api.Test;
import use_case.login.LoginOutputData;

import static org.junit.jupiter.api.Assertions.*;

class LoginPresenterTest {

    @Test
    void successTest() {
        LoginViewModel viewModel = new LoginViewModel();
        LoginPresenter presenter =
                new LoginPresenter(viewModel);

        LoginOutputData outputData =
                new LoginOutputData("testuser");

        presenter.prepareSuccessView(outputData);

        LoginState state = viewModel.getState();

        assertEquals("testuser", state.getUsername());
        assertNull(state.getLoginError());
    }

    @Test
    void failureTest() {
        LoginViewModel viewModel = new LoginViewModel();
        LoginPresenter presenter =
                new LoginPresenter(viewModel);

        presenter.prepareFailView("Incorrect password.");

        LoginState state = viewModel.getState();

        assertEquals(
                "Incorrect password.",
                state.getLoginError());
    }

    @Test
    void successAfterFailureTest() {
        LoginViewModel viewModel = new LoginViewModel();
        LoginPresenter presenter =
                new LoginPresenter(viewModel);

        presenter.prepareFailView("Incorrect password.");

        presenter.prepareSuccessView(
                new LoginOutputData("testuser"));

        LoginState state = viewModel.getState();

        assertEquals("testuser", state.getUsername());
        assertNull(state.getLoginError());
    }
}
