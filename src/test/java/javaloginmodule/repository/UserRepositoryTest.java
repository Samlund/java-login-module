package javaloginmodule.repository;

import javaloginmodule.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertFalse;
import static org.springframework.test.util.AssertionErrors.assertTrue;

@ActiveProfiles("test")
@SpringBootTest
public class UserRepositoryTest {

    @Autowired
    private UserRepository repository;

    @BeforeEach
    public void setUp(@Autowired JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("INSERT INTO users (username, password) VALUES ('sam', 'hashed123')");
    }

    @Test
    public void fetchByUsername_returnsUser_ifUserExists() {
        String username = "sam";
        Optional<User> user = repository.fetchByUsername(username);
        assertAll(
                () -> assertTrue("User not found for username: " + username, user.isPresent()),
                () -> assertEquals("sam", user.get().username())
        );
    }

    @Test
    public void fetchByUsername_returnsEmpty_ifUserDoesNotExist() {
        String username = "pedro";
        Optional<User> user = repository.fetchByUsername(username);
        assertTrue("Expected empty Optional for non existing user", user.isEmpty());
    }

    @Test
    public void save_returnsUser_ifUserIsAdded() {
        String username = "alex";
        User user = new User(0, username, "hashed321");

        Optional<User> savedUser = repository.save(user);

        assertAll(
                () -> assertTrue("Failed to create user", savedUser.isPresent()),
                () -> assertEquals(username, savedUser.get().username()),
                () -> assertEquals("hashed321", savedUser.get().passwordHash())
        );
    }

    @Test
    public void save_returnsEmpty_ifUserAlreadyExist() {
        String username = "sam";
        User user = new User(0, username, "hashBrown");

        Optional<User> savedUser = repository.save(user);

        assertTrue("Expected save to fail for existing user", savedUser.isEmpty());
    }

    @Test
    public void update_returnsUser_ifUserExists() {
        Optional<User> user = repository.fetchByUsername("sam");
        String newPassword = "newHash789";
        User userToUpdate = new User(user.get().id(), user.get().username(), newPassword);

        Optional<User> updatedUser = repository.update(userToUpdate);

        assertAll(
                () -> assertTrue("Failed to update password", updatedUser.isPresent()),
                () -> assertEquals(userToUpdate.passwordHash(), updatedUser.get().passwordHash())
        );
    }

    @Test
    public void update_returnsEmpty_ifUserDoesNotExist() {
        User user = new User(0, "peter", "hashCake");
        User userToUpdate = new User(user.id(), user.username(), user.passwordHash());

        Optional<User> updatedUser = repository.update(userToUpdate);

        assertTrue("Expected update to fail for non-existing user", updatedUser.isEmpty());
    }

    @Test
    public void delete_returnsTrue_ifUserExists() {
        Optional<User> user = repository.fetchByUsername("sam");

        assertAll(
                () -> assertTrue("Failed to delete user", repository.delete(user.get())),
                () -> assertTrue("User targeted for deletion still exists", repository.fetchByUsername("sam").isEmpty())
        );
    }

    @Test
    public void delete_returnsFalse_ifUserDoesNotExist() {
        User user = new User(0, "peter", "hashCake");

        assertAll(
                () -> assertTrue("Sanity check failed: user should not exist", repository.fetchByUsername("peter").isEmpty()),
                () -> assertFalse("Expected delete to fail for non-existing user", repository.delete(user))
        );
    }

    @Test
    public void save_returnsEmpty_ifUsernameIsNull() {
        User user = new User(0, null, "hash123");
        Optional<User> savedUser = repository.save(user);
        assertTrue("Expected save to fail when username is null", savedUser.isEmpty());
    }

    @Test
    public void save_returnsEmpty_ifPasswordIsNull() {
        User user = new User(0, "charlie", null);
        Optional<User> savedUser = repository.save(user);
        assertTrue("Expected save to fail when password is null", savedUser.isEmpty());
    }
}
