package javaloginmodule.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PasswordHasherTest {
    @Autowired
    private PasswordHasher passwordHasher;

    @Test
    public void hash_returnsNonNullAndDifferent() {
        String password = "hashBrown123";
        String hash = passwordHasher.hash(password);

        assertAll(
                () -> assertNotNull(hash, "Hash should not be null"),
                () -> assertNotEquals(password, hash, "Hash should be different from plaintext password")
        );
    }

    @Test
    public void verify_returnsTrue_ifPasswordMatches() {
        String password = "dummyHash123";
        String hash = passwordHasher.hash(password);

        assertTrue(passwordHasher.verify(password, hash), "Candidate password did not match hashed password");
    }

    @Test
    public void verify_returnFalse_ifPasswordDoesNotMatch() {
        String password = "dummyHash321";
        String hash = passwordHasher.hash("dummyHash123");

        assertFalse(passwordHasher.verify(password, hash), "Expected comparison to fail for non-matching passwords");
    }

    @Test
    public void hash_throwsException_ifPasswordIsNull() {
        assertThrows(IllegalArgumentException.class, () -> passwordHasher.hash(null));
    }

}
