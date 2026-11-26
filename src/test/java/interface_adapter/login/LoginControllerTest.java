package interface_adapter.login;

import org.junit.jupiter.api.Test;
import use_case.login.LoginInputBoundary;
import use_case.login.LoginInputData;

import static org.junit.jupiter.api.Assertions.*;

class LoginControllerTest {

    @Test
    void executeTest() {
        FakeLoginInteractor fakeInteractor =
                new FakeLoginInteractor();

        LoginController controller =
                new LoginController(fakeInteractor);

        controller.execute("testuser", "password123");

        assertTrue(fakeInteractor.executed);
        assertEquals(
                "testuser",
                fakeInteractor.receivedInput.getUsername());
        assertEquals(
                "password123",
                fakeInteractor.receivedInput.getPassword());
    }

    private static class FakeLoginInteractor
            implements LoginInputBoundary {

        boolean executed = false;
        LoginInputData receivedInput;

        @Override
        public void execute(LoginInputData inputData) {
            executed = true;
            receivedInput = inputData;
        }
    }
}
