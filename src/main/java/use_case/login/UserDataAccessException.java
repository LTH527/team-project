package use_case.login;

public class UserDataAccessException extends RuntimeException {

    public UserDataAccessException(String message) {
        super(message);
    }

    public UserDataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
