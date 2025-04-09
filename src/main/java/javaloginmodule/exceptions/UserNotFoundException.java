package javaloginmodule.exceptions;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String username) {
        super("User with username '" + username + "' not found");
    }

    public UserNotFoundException(int id) {
        super("User with ID '" + id + "' not found");
    }
}
