package javaloginmodule.service;

import javaloginmodule.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import javaloginmodule.repository.UserRepository;
import javaloginmodule.security.PasswordHasher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.util.AssertionErrors.assertFalse;
import static org.springframework.test.util.AssertionErrors.assertTrue;

@ActiveProfiles("test")
@SpringBootTest
public class AuthServiceTest {

    private final UserRepository repository;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;
    private final JdbcTemplate jdbcTemplate;
    private AuthService service;

    @Autowired
    public AuthServiceTest(UserRepository repository, PasswordHasher passwordHasher, TokenService tokenService, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    public void setUp() {
        service = new AuthService(repository, passwordHasher, tokenService);
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    public void register_returnUserDetailsResponse_ifUserDoesNotExist() {
        String username = "sam";
        UserRequest request = new UserRequest(username, "password123");

        Optional<UserDetailsResponse> registration = service.register(request);

        assertAll(
                () -> assertTrue("Could not register user: " + username, registration.isPresent()),
                () -> assertEquals(username, registration.get().username())
        );
    }

    @Test
    public void register_returnEmptyResponse_ifUserAlreadyExists() {
        String username = "sam";
        UserRequest request = new UserRequest(username, "password123");

        Optional<UserDetailsResponse> registration = service.register(request);

        String password = "anotherPassword123";
        request = new UserRequest(username, password);
        Optional<UserDetailsResponse> duplicateRegistration = service.register(request);

        assertAll(
                () -> assertTrue("Failed to create user", registration.isPresent()),
                () -> assertTrue("Expected response to be empty", duplicateRegistration.isEmpty())
        );
    }

    @Test
    public void authenticate_returnAuthResponse_ifValidCredentials() {
        String username = "sam";
        UserRequest request = new UserRequest(username, "password123");

        Optional<UserDetailsResponse> registration = service.register(request);
        Optional<AuthResponse> authentication = service.authenticate(request);

        assertAll(
                () -> assertTrue("Could not mock user", registration.isPresent()),
                () -> assertTrue("Failed to authenticate user", authentication.isPresent())
        );
    }

    @Test
    public void authenticate_returnEmptyResponse_ifInvalidCredentials() {
        String username = "sam";
        UserRequest request = new UserRequest(username, "password123");

        Optional<UserDetailsResponse> registration = service.register(request);

        String wrongPassword = "abc123";
        UserRequest badRequest = new UserRequest(username, wrongPassword);
        Optional<AuthResponse> authentication = service.authenticate(badRequest);

        assertAll(
                () -> assertTrue("Could not mock user", registration.isPresent()),
                () -> assertTrue("Failed to authenticate user", authentication.isEmpty())
        );
    }

    @Test
    public void authenticate_returnAuthResponseWithToken_ifValidCredentials() {
        String username = "sam";
        UserRequest request = new UserRequest(username, "password123");

        Optional<UserDetailsResponse> registration = service.register(request);

        Optional<AuthResponse> authentication = service.authenticate(request);

        assertAll(
                () -> assertTrue("Could not mock user", registration.isPresent()),
                () -> assertTrue("Failed to authenticate user", authentication.isPresent()),
                () -> assertNotNull(authentication.get().token(), "Token should not be null")
        );
    }

    @Test
    public void updatePassword_returnAuthResponse_ifAuthenticatedAndUserExists() {
        String username = "sam";
        UserRequest request = new UserRequest(username, "password123");

        Optional<UserDetailsResponse> registration = service.register(request);

        Optional<User> user = repository.fetchByUsername(username);
        Assertions.assertTrue(user.isPresent());

        Token token = new Token(tokenService.generateToken(user.get()));

        String newPassword = "newPassword321";
        Optional<AuthResponse> updated = service.updatePassword(token, newPassword);

        assertAll(
                () -> assertTrue("Could not mock user", registration.isPresent()),
                () -> assertTrue("Failed to update user information", updated.isPresent())
        );
    }

    @Test
    public void updatePassword_returnAuthResponseWithUpdatedPassword() {
        String username = "sam";
        UserRequest request = new UserRequest(username, "password123");

        Optional<UserDetailsResponse> registration = service.register(request);

        Optional<User> user = repository.fetchByUsername(username);
        Assertions.assertTrue(user.isPresent());

        Token token = new Token(tokenService.generateToken(user.get()));

        String newPassword = "newPassword321";
        Optional<AuthResponse> updated = service.updatePassword(token, newPassword);

        Optional<User> updatedUser = repository.fetchByUsername(username);

        assertAll(
                () -> assertTrue("Could not mock user", registration.isPresent()),
                () -> assertTrue("Failed to update user information", updated.isPresent()),
                () -> assertTrue("User not found after updating", updatedUser.isPresent()),
                () -> assertTrue("Passwords do not match after update",
                        passwordHasher.verify(newPassword, updatedUser.get().passwordHash()))
        );
    }

    @Test
    public void delete_returnTrue_ifUserExists() {
        String username = "sam";
        UserRequest request = new UserRequest(username, "password123");

        Optional<UserDetailsResponse> registration = service.register(request);

        Optional<User> user = repository.fetchByUsername(username);
        Assertions.assertTrue(user.isPresent());

        Token token = new Token(tokenService.generateToken(user.get()));

        assertAll(
                () -> assertTrue("Could not mock user", registration.isPresent()),
                () -> assertTrue("Failed to delete user", service.delete(token))
        );
    }

    @Test
    public void delete_returnFalse_ifUserDoesNotExist() {
        String username = "sam";
        Token token = new Token(tokenService.generateToken(new User(1, username, "password123")));

        assertFalse("User targeted for deletion still exists", service.delete(token));
    }
}
