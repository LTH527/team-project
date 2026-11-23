
package use_case.login;

import entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginInteractorTest {

    @Test
    void successTest() {
        FakeUserDAO dao = new FakeUserDAO();
        dao.userExists = true;
        dao.storedPassword = "password123";

        FakePresenter presenter = new FakePresenter();
        LoginInteractor interactor =
                new LoginInteractor(dao, presenter);

        interactor.execute(
                new LoginInputData("testuser", "password123"));

        assertTrue(presenter.success);
        assertNull(presenter.error);
        assertEquals("testuser", dao.currentUsername);
        assertEquals("testuser",
                presenter.outputData.getUsername());
    }

    @Test
    void wrongPasswordTest() {
        FakeUserDAO dao = new FakeUserDAO();
        dao.userExists = true;
        dao.storedPassword = "correctPassword";

        FakePresenter presenter = new FakePresenter();
        LoginInteractor interactor =
                new LoginInteractor(dao, presenter);

        interactor.execute(
                new LoginInputData("testuser", "wrongPassword"));

        assertFalse(presenter.success);
        assertEquals("Incorrect password.", presenter.error);
        assertNull(dao.currentUsername);
    }

    @Test
    void userNotFoundTest() {
        FakeUserDAO dao = new FakeUserDAO();
        dao.userExists = false;

        FakePresenter presenter = new FakePresenter();
        LoginInteractor interactor =
                new LoginInteractor(dao, presenter);

        interactor.execute(
                new LoginInputData("unknown", "password123"));

        assertFalse(presenter.success);
        assertEquals("User does not exist.", presenter.error);
        assertNull(dao.currentUsername);
    }

    @Test
    void networkErrorDuringExistsTest() {
        FakeUserDAO dao = new FakeUserDAO();
        dao.throwOnExists = true;

        FakePresenter presenter = new FakePresenter();
        LoginInteractor interactor =
                new LoginInteractor(dao, presenter);

        interactor.execute(
                new LoginInputData("testuser", "password123"));

        assertFalse(presenter.success);
        assertEquals(
                "Unable to connect to the user service. Please try again.",
                presenter.error);
        assertNull(presenter.outputData);
        assertNull(dao.currentUsername);
    }

    @Test
    void networkErrorDuringGetTest() {
        FakeUserDAO dao = new FakeUserDAO();
        dao.userExists = true;
        dao.throwOnGet = true;

        FakePresenter presenter = new FakePresenter();
        LoginInteractor interactor =
                new LoginInteractor(dao, presenter);

        interactor.execute(
                new LoginInputData("testuser", "password123"));

        assertFalse(presenter.success);
        assertEquals(
                "Unable to connect to the user service. Please try again.",
                presenter.error);
        assertNull(presenter.outputData);
        assertNull(dao.currentUsername);
    }

    private static class FakeUserDAO
            implements LoginUserDataAccessInterface {

        boolean userExists;
        String storedPassword;
        String currentUsername;
        boolean throwOnExists;
        boolean throwOnGet;

        @Override
        public boolean existsByName(String username) {
            if (throwOnExists) {
                throw new UserDataAccessException("API connection failed.");
            }
            return userExists;
        }

        @Override
        public User get(String username) {
            if (throwOnGet) {
                throw new UserDataAccessException("API connection failed.");
            }
            return new User(username, storedPassword);
        }

        @Override
        public void setCurrentUsername(String username) {
            currentUsername = username;
        }

        @Override
        public String getCurrentUsername() {
            return currentUsername;
        }
    }

    private static class FakePresenter
            implements LoginOutputBoundary {

        boolean success;
        String error;
        LoginOutputData outputData;

        @Override
        public void prepareSuccessView(LoginOutputData data) {
            success = true;
            outputData = data;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            success = false;
            error = errorMessage;
        }
    }
}
