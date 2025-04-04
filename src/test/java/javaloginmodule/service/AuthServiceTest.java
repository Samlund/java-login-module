package javaloginmodule.service;

import javaloginmodule.model.AuthRequest;
import javaloginmodule.model.AuthResponse;
import javaloginmodule.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import javaloginmodule.repository.UserRepository;
import javaloginmodule.security.PasswordHasher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertFalse;
import static org.springframework.test.util.AssertionErrors.assertTrue;

@ActiveProfiles("test")
@SpringBootTest
public class AuthServiceTest {

    private final UserRepository repository;
    private final PasswordHasher passwordHasher;
    private final JdbcTemplate jdbcTemplate;
    private AuthService service;

    @Autowired
    public AuthServiceTest(UserRepository repository, PasswordHasher passwordHasher, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    public void setUp() {
        service = new AuthService(repository, passwordHasher);
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    public void register_returnAuthResponse_ifUserDoesNotExist() {
        String username = "sam";
        String password = "password123";
        AuthRequest authRequest = new AuthRequest(username, password);

        Optional<AuthResponse> authResponse = service.register(authRequest);

        assertAll(
                () -> assertTrue("Could not register user: " + username, authResponse.isPresent()),
                () -> assertEquals(username, authResponse.get().username())
        );
    }

    @Test
    public void register_returnEmptyAuthResponse_ifUserAlreadyExists() {
        String username = "sam";
        String password = "password123";
        AuthRequest authRequest = new AuthRequest(username, password);

        Optional<AuthResponse> authResponse = service.register(authRequest);

        password = "anotherPassword123";
        authRequest = new AuthRequest(username, password);
        Optional<AuthResponse> duplicateAuthResponse = service.register(authRequest);

        assertAll(
                () -> assertTrue("Failed to create user", authResponse.isPresent()),
                () -> assertTrue("Expected response to be empty", duplicateAuthResponse.isEmpty())
        );
    }

    @Test
    public void authenticate_returnAuthResponse_ifValidCredentials() {
        String username = "sam";
        String password = "password123";
        AuthRequest authRequest = new AuthRequest(username, password);

        Optional<AuthResponse> registrationResponse = service.register(authRequest);
        Optional<AuthResponse> authenticatedResponse = service.authenticate(authRequest);

        assertAll(
                () -> assertTrue("Could not mock user", registrationResponse.isPresent()),
                () -> assertTrue("Failed to authenticate user", authenticatedResponse.isPresent())
        );
    }

    @Test
    public void authenticate_returnEmpty_ifInvalidCredentials() {
        String username = "sam";
        String password = "password123";
        AuthRequest authRequest = new AuthRequest(username, password);

        Optional<AuthResponse> registrationResponse = service.register(authRequest);

        String wrongPassword = "abc123";
        AuthRequest badAuthRequest = new AuthRequest(username, wrongPassword);
        Optional<AuthResponse> authenticatedResponse = service.authenticate(badAuthRequest);

        assertAll(
                () -> assertTrue("Could not mock user", registrationResponse.isPresent()),
                () -> assertTrue("Failed to authenticate user", authenticatedResponse.isEmpty())
        );
    }

    @Test
    public void update_returnAuthResponse_ifAuthenticatedAndUserExists() {
        String username = "sam";
        String password = "password123";
        AuthRequest authRequest = new AuthRequest(username, password);

        Optional<AuthResponse> registrationResponse = service.register(authRequest);

        String newPassword = "newPassword321";
        Optional<AuthResponse> updatedResponse = service.update(authRequest, newPassword);

        assertAll(
                () -> assertTrue("Could not mock user", registrationResponse.isPresent()),
                () -> assertTrue("Failed to update user information", updatedResponse.isPresent())
        );
    }

    @Test
    public void update_returnAuthResponseWithUpdatedPassword() {
        String username = "sam";
        String password = "password123";
        AuthRequest authRequest = new AuthRequest(username, password);

        Optional<AuthResponse> registrationResponse = service.register(authRequest);

        String newPassword = "newPassword321";
        Optional<AuthResponse> updatedResponse = service.update(authRequest, newPassword);

        Optional<User> updatedUser = repository.fetchByUsername(username);

        assertAll(
                () -> assertTrue("Could not mock user", registrationResponse.isPresent()),
                () -> assertTrue("Failed to update user information", updatedResponse.isPresent()),
                () -> assertTrue("User not found after updating", updatedUser.isPresent()),
                () -> assertTrue("Passwords do not match after update",
                        passwordHasher.verify(newPassword, updatedUser.get().passwordHash()))
        );
    }

    @Test
    public void delete_returnTrue_ifUserExists() {
        String username = "sam";
        String password = "password123";
        AuthRequest authRequest = new AuthRequest(username, password);

        Optional<AuthResponse> registrationResponse = service.register(authRequest);

        assertAll(
                () -> assertTrue("Could not mock user", registrationResponse.isPresent()),
                () -> assertTrue("Failed to delete user", service.delete(authRequest))
        );
    }

    @Test
    public void delete_returnFalse_ifUserDoesNotExist() {
        String username = "sam";
        String password = "password123";
        AuthRequest authRequest = new AuthRequest(username, password);

        assertFalse("User targeted for deletion still exists", service.delete(authRequest));
    }
}
