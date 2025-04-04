package javaloginmodule.service;

import javaloginmodule.model.AuthRequest;
import javaloginmodule.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import javaloginmodule.repository.UserRepository;
import javaloginmodule.security.PasswordHasher;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.springframework.test.util.AssertionErrors.assertTrue;

@ActiveProfiles("test")
@SpringBootTest
public class AuthServiceTest {

    @Autowired
    private UserRepository repository;
    @Autowired
    private PasswordHasher passwordHasher;

    @Test
    public void registerUserTest() {
        String username = "sam";
        String password = "password123";

        AuthRequest authRequest = new AuthRequest(username, password);
        AuthService service = new AuthService(repository, passwordHasher);

        service.register(authRequest);

        Optional<User> savedUser = repository.fetchByUsername(username);
        assertTrue("Could not register user: " + username, savedUser.isPresent());
    }
}
