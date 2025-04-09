package javaloginmodule.service;

import javaloginmodule.model.User;
import javaloginmodule.security.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.springframework.test.util.AssertionErrors.assertEquals;

@SpringBootTest
public class TokenServiceTest {

    @Autowired
    private TokenService tokenService;

    @Test
    public void generateToken_returnsToken_ifValidCredentials() {
        User user = new User(1, "sam", "hashedPassword");

        String token = tokenService.generateToken(user);

        assertAll(
                () -> assertNotNull(token, "Token should not be null"),
                () -> assertFalse(token.isBlank(), "Token should not be blank")
        );
    }

    @Test
    public void verifyToken_returnsSubject_ifValidToken() {
        User user = new User(1, "sam", "hashedPassword");

        String token = tokenService.generateToken(user);

        Optional<String> result = tokenService.verifyToken(token);

        assertAll(
                () -> assertTrue("Expected token to be valid", result.isPresent()),
                () -> assertEquals("Expected subject to be user ID as String", String.valueOf(user.id()), result.get())
        );
    }
}
