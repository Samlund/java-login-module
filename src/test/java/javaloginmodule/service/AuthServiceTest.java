package javaloginmodule.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import javaloginmodule.exceptions.*;
import javaloginmodule.model.*;
import javaloginmodule.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import javaloginmodule.repository.UserRepository;
import javaloginmodule.security.PasswordHasher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

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

        UserDetailsResponse registration = service.register(request);

        assertAll(
                () -> assertEquals(username, registration.username()),
                () -> assertTrue(registration.id() > 0, "Expected id to be a positive value"),
                () -> assertNotNull(registration.createdAt())
        );
    }

    @Test
    public void register_throwsUserAlreadyExistsException_ifUserAlreadyExists() {
        String username = "sam";
        UserRequest request = new UserRequest(username, "password123");

        UserDetailsResponse registration = service.register(request);
        assertEquals(username, registration.username());

        String password = "anotherPassword123";
        UserRequest duplicateRequest = new UserRequest(username, password);

        assertThrows(UserAlreadyExistsException.class, () -> service.register(duplicateRequest));
    }

    @Test
    public void register_throwsBadRequestException_ifPasswordIsNull() {
        UserRequest request = new UserRequest(null, "password123");
        assertThrows(BadRequestException.class, () -> service.register(request));
    }

    @Test
    public void register_throwsBadRequestException_ifUsernameIsNull() {
        UserRequest request = new UserRequest("sam", null);
        assertThrows(BadRequestException.class, () -> service.register(request));
    }

    @Test
    public void authenticate_returnAuthResponse_ifValidCredentials() {
        String username = "sam";
        UserRequest request = new UserRequest(username, "password123");

        UserDetailsResponse registration = service.register(request);
        assertEquals(username, registration.username());

        AuthResponse authentication = service.authenticate(request);

        assertAll(
                () -> assertEquals(username, authentication.user().username()),
                () -> assertNotNull(authentication.token().value())
        );
    }

    @Test
    public void authenticate_throwsInvalidCredentialsException_ifInvalidCredentials() {
        String username = "sam";
        UserRequest request = new UserRequest(username, "password123");

        UserDetailsResponse registration = service.register(request);
        assertEquals(username, registration.username());

        String wrongPassword = "abc123";
        UserRequest badRequest = new UserRequest(username, wrongPassword);

        assertThrows(InvalidCredentialsException.class, () -> service.authenticate(badRequest));
    }

    @Test
    public void updatePassword_returnAuthResponse_ifAuthenticatedAndUserExists() {
        String username = "sam";
        UserRequest request = new UserRequest(username, "password123");

        UserDetailsResponse registration = service.register(request);
        assertEquals(username, registration.username());

        User user = repository.fetchByUsername(username);
        assertEquals(username, user.username());

        Token token = new Token(tokenService.generateToken(user));

        String newPassword = "newPassword321";
        AuthResponse updated = service.updatePassword(token, newPassword);

        assertAll(
                () -> assertEquals(username, updated.user().username(), "Expected username to remain the same"),
                () -> assertNotNull(updated.token().value(), "Expected a new token to be generated"),
                () -> {
                    User updatedUser = repository.fetchByUsername(username);
                    assertTrue(passwordHasher.verify(newPassword, updatedUser.passwordHash()), "Expected password to be updated");
                }
        );
    }

    @Test
    public void updatePassword_throwsUnauthorizedAccessException_ifTokenIsInvalid() {
        Token token = new Token("invalid.token.value");
        String newPassword = "whatever";

        assertThrows(UnauthorizedAccessException.class, () ->
                service.updatePassword(token, newPassword)
        );
    }

    @Test
    public void updatePassword_throwsUserNotFoundException_ifUserIdNotFound() {
        Token token = new Token(tokenService.generateToken(new User(999, "ghost", "ghostHash")));
        String newPassword = "newPassword";

        assertThrows(UserNotFoundException.class, () ->
                service.updatePassword(token, newPassword)
        );
    }

    @Test
    public void updatePassword_throwsUnauthorizedAccessException_ifTokenSubjectIsNotInteger() {
        String token = JWT.create()
                .withSubject("notAnInt")
                .withIssuer("login-app")
                .sign(Algorithm.HMAC256("demo-secret"));

        assertThrows(UnauthorizedAccessException.class, () ->
                service.updatePassword(new Token(token), "anyPassword")
        );
    }

    @Test
    public void delete_throwsUnauthorizedAccess_ifTokenIsInvalid() {
        Token token = new Token("not-a-real-jwt");
        assertThrows(UnauthorizedAccessException.class, () -> service.delete(token));
    }

    @Test
    public void delete_throwsUnauthorizedAccess_ifTokenIsExpired() {
        Algorithm algorithm = Algorithm.HMAC256("demo-secret");
        String expiredToken = JWT.create()
                .withSubject("1")
                .withIssuer("login-app")
                .withExpiresAt(Date.from(Instant.now().minus(1, ChronoUnit.HOURS)))
                .sign(algorithm);

        Token token = new Token(expiredToken);
        assertThrows(UnauthorizedAccessException.class, () -> service.delete(token));
    }


    @Test
    public void delete_deletesUser_ifUserExists() {
        String username = "sam";
        UserRequest request = new UserRequest(username, "password123");

        UserDetailsResponse registration = service.register(request);

        User user = repository.fetchByUsername(registration.username());
        assertEquals(username, user.username());

        Token token = new Token(tokenService.generateToken(user));

        service.delete(token);

        assertThrows(UserNotFoundException.class, () -> repository.fetchByUsername(user.username()));
    }

    @Test
    public void delete_throwsUnauthorizedAccessException_ifUserDoesNotExist() {
        String username = "sam";
        Token token = new Token(tokenService.generateToken(new User(1, username, "password123")));

        assertThrows(UnauthorizedAccessException.class, () -> service.delete(token));
    }
}
