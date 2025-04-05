package javaloginmodule.service;

import javaloginmodule.model.AuthRequest;
import javaloginmodule.model.AuthResponse;
import javaloginmodule.model.User;
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
    private UserRepository userRepository;

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
    public void register_returnAuthResponse_ifUserDoesNotExist() {
        String username = "sam";
        String password = "password123";
        AuthRequest request = AuthRequest.withoutToken(username, password);

        Optional<AuthResponse> registration = service.register(request);

        assertAll(
                () -> assertTrue("Could not register user: " + username, registration.isPresent()),
                () -> assertEquals(username, registration.get().username())
        );
    }

    @Test
    public void register_returnEmptyAuthResponse_ifUserAlreadyExists() {
        String username = "sam";
        String password = "password123";
        AuthRequest request = AuthRequest.withoutToken(username, password);

        Optional<AuthResponse> registration = service.register(request);

        password = "anotherPassword123";
        request = AuthRequest.withoutToken(username, password);
        Optional<AuthResponse> duplicateRegistration = service.register(request);

        assertAll(
                () -> assertTrue("Failed to create user", registration.isPresent()),
                () -> assertTrue("Expected response to be empty", duplicateRegistration.isEmpty())
        );
    }

    @Test
    public void authenticate_returnAuthResponse_ifValidCredentials() {
        String username = "sam";
        String password = "password123";
        AuthRequest request = AuthRequest.withoutToken(username, password);

        Optional<AuthResponse> registration = service.register(request);
        Optional<AuthResponse> authentication = service.authenticate(request);

        assertAll(
                () -> assertTrue("Could not mock user", registration.isPresent()),
                () -> assertTrue("Failed to authenticate user", authentication.isPresent())
        );
    }

    @Test
    public void authenticate_returnEmpty_ifInvalidCredentials() {
        String username = "sam";
        String password = "password123";
        AuthRequest request = AuthRequest.withoutToken(username, password);

        Optional<AuthResponse> registration = service.register(request);

        String wrongPassword = "abc123";
        AuthRequest badRequest = AuthRequest.withoutToken(username, wrongPassword);
        Optional<AuthResponse> authentication = service.authenticate(badRequest);

        assertAll(
                () -> assertTrue("Could not mock user", registration.isPresent()),
                () -> assertTrue("Failed to authenticate user", authentication.isEmpty())
        );
    }

    @Test
    public void update_returnAuthResponse_ifAuthenticatedAndUserExists() {
        String username = "sam";
        String password = "password123";
        AuthRequest request = AuthRequest.withoutToken(username, password);

        Optional<AuthResponse> registration = service.register(request);

        Optional<User> user = userRepository.fetchByUsername(username);
        Assertions.assertTrue(user.isPresent());

        String token = tokenService.generateToken(new User(user.get().id(), user.get().username(), user.get().passwordHash()));
        AuthRequest requestWithToken = AuthRequest.withToken(username, token);

        String newPassword = "newPassword321";
        Optional<AuthResponse> updated = service.update(requestWithToken, newPassword);

        assertAll(
                () -> assertTrue("Could not mock user", registration.isPresent()),
                () -> assertTrue("Failed to update user information", updated.isPresent())
        );
    }

    @Test
    public void update_returnAuthResponseWithUpdatedPassword() {
        String username = "sam";
        String password = "password123";
        AuthRequest request = AuthRequest.withoutToken(username, password);

        Optional<AuthResponse> registration = service.register(request);

        Optional<User> user = userRepository.fetchByUsername(username);
        Assertions.assertTrue(user.isPresent());

        String token = tokenService.generateToken(new User(user.get().id(), user.get().username(), user.get().passwordHash()));
        AuthRequest requestWithToken = AuthRequest.withToken(username, token);

        String newPassword = "newPassword321";
        Optional<AuthResponse> updated = service.update(requestWithToken, newPassword);

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
        String password = "password123";
        AuthRequest request = AuthRequest.withoutToken(username, password);

        Optional<AuthResponse> registration = service.register(request);

        Optional<User> user = userRepository.fetchByUsername(username);
        Assertions.assertTrue(user.isPresent());

        String token = tokenService.generateToken(new User(user.get().id(), user.get().username(), user.get().passwordHash()));
        AuthRequest requestWithToken = AuthRequest.withToken(username, token);

        assertAll(
                () -> assertTrue("Could not mock user", registration.isPresent()),
                () -> assertTrue("Failed to delete user", service.delete(requestWithToken))
        );
    }

    @Test
    public void delete_returnFalse_ifUserDoesNotExist() {
        String username = "sam";
        String password = "password123";
        String token = tokenService.generateToken(new User(1, username, password));
        AuthRequest request = AuthRequest.withToken(username, token);

        assertFalse("User targeted for deletion still exists", service.delete(request));
    }

    @Test
    public void authenticate_returnAuthResponseWithToken_ifValidCredentials() {
        String username = "sam";
        String password = "password123";
        AuthRequest request = AuthRequest.withoutToken(username, password);

        Optional<AuthResponse> registration = service.register(request);

        Optional<AuthResponse> authentication = service.authenticate(request);

        assertAll(
                () -> assertTrue("Could not mock user", registration.isPresent()),
                () -> assertTrue("Failed to authenticate user", authentication.isPresent()),
                () -> assertNotNull(authentication.get().token(), "Token should not be null")
        );
    }
}
