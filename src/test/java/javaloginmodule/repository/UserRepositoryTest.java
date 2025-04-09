package javaloginmodule.repository;

import javaloginmodule.exceptions.UserAlreadyExistsException;
import javaloginmodule.exceptions.UserNotFoundException;
import javaloginmodule.model.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
//import static org.springframework.test.util.AssertionErrors.*;

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
        User user = repository.fetchByUsername(username);
        assertEquals("sam", user.username());
    }

    @Test
    public void fetchByUsername_throwsUserNotFoundException_ifUserDoesNotExist() {
        String username = "pedro";
        assertThrows(UserNotFoundException.class, () -> repository.fetchByUsername(username));
    }

    @Test
    public void save_returnsUser_ifUserIsAdded() {
        String username = "alex";
        User user = new User(0, username, "hashed321");

        User savedUser = repository.save(user);

        assertAll(
                () -> assertEquals(username, savedUser.username()),
                () -> assertEquals("hashed321", savedUser.passwordHash())
        );
    }

    @Test
    public void save_throwsUserAlreadyExistsException_ifUserAlreadyExist() {
        String username = "sam";
        User user = new User(0, username, "hashBrown");
        assertThrows(UserAlreadyExistsException.class, () -> repository.save(user));
    }

    @Test
    public void update_returnsUser_ifUserExists() {
        User user = repository.fetchByUsername("sam");
        String newPassword = "newHash789";
        User userToUpdate = new User(user.id(), user.username(), newPassword);

        User updatedUser = repository.update(userToUpdate);

        assertEquals(userToUpdate.passwordHash(), updatedUser.passwordHash(), "Expected password to be updated");
    }

    @Test
    public void update_throwsUserNotFoundException_ifUserDoesNotExist() {
        User user = new User(0, "peter", "hashCake");
        assertThrows(UserNotFoundException.class, () -> repository.update(user));
    }

    @Test
    public void delete_deletesUser_ifUserExists() {
        String username = "sam";
        User user = repository.fetchByUsername(username);
        assertEquals(username, user.username());

        repository.delete(user.id());

        assertThrows(UserNotFoundException.class, () -> repository.fetchByUsername(username), "Expected UserNotFoundException after deleting user");
    }

    @Test
    public void delete_throwsUserNotFoundException_ifUserDoesNotExist() {
        User user = new User(0, "peter", "hashCake");
        assertThrows(UserNotFoundException.class, () -> repository.delete(user.id()));
    }

    @Test
    void fetchById_returnsUser_ifUserExists() {
        Optional<User> result = repository.fetchById(1);

        assertAll(
                () -> assertTrue(result.isPresent(), "Expected user to be present"),
                () -> assertEquals(1, result.get().id()),
                () -> assertEquals("sam", result.get().username()),
                () -> assertEquals("hashed123", result.get().passwordHash())
        );
    }

    @Test
    void fetchById_returnsEmpty_ifUserDoesNotExist() {
        Optional<User> result = repository.fetchById(999);

        assertTrue(result.isEmpty(), "Expected Optional to be empty for non-existent user");
    }

    @Test
    public void getUserCreationTimestamp_returnsTimestamp_ifIdExists() {
        User user = new User(0, "peter", "hashCake");
        User savedUser = repository.save(user);

        Optional<LocalDateTime> timestamp = repository.getUserCreationTimestamp(savedUser.id());

        assertAll(
                () -> assertTrue(timestamp.isPresent(), "Expected timestamp to exist"),
                () -> {
                    LocalDateTime now = LocalDateTime.now();
                    Duration diff = Duration.between(timestamp.get(), now);
                    assertTrue(Math.abs(diff.toMillis()) < 100, "Timestamp is too far off current time");
                }
        );
    }
}
