package javaloginmodule.repository;

import javaloginmodule.exceptions.UserAlreadyExistsException;
import javaloginmodule.exceptions.UserNotFoundException;
import javaloginmodule.model.User;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public User save(User user) {
        String sql = "INSERT INTO users (username, password) VALUES (?,?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
                ps.setString(1, user.username());
                ps.setString(2, user.passwordHash());
                return ps;
            }, keyHolder);

            Map<String, Object> keys = keyHolder.getKeys();
            int id = ((Number) keys.get("ID")).intValue();

            return new User(id, user.username(), user.passwordHash());
        } catch (DuplicateKeyException e) {
            throw new UserAlreadyExistsException(user.username());
        }
    }

    public User update(User user) {
        String sql = "UPDATE users SET password = ? WHERE username = ?";

        int rowsAffected = jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, user.passwordHash());
            ps.setString(2, user.username());
            return ps;
        });

        if (rowsAffected == 0) {
            throw new UserNotFoundException(user.username());
        }

        return fetchByUsername(user.username());
    }

    public void delete(int id) {
        String sql = "DELETE FROM users WHERE id = ?";
        int rowsAffected = jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);
            return ps;
        });

        if (rowsAffected == 0) {
            throw new UserNotFoundException(id);
        }
    }

    public User fetchByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try {
            return jdbcTemplate.queryForObject(
                    sql,
                    (rs, rowNum) -> new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password")
                    ),
                    username
            );
        } catch (EmptyResultDataAccessException e) {
            throw new UserNotFoundException(username);
        }
    }

    public Optional<User> fetchById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try {
            User user = jdbcTemplate.queryForObject(
                    sql,
                    (rs, rowNum) -> new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password")
                    ),
                    id
            );
            return Optional.of(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<LocalDateTime> getUserCreationTimestamp(int id) {
        String sql = "SELECT created_at FROM users WHERE id = ?";
        try {
            Timestamp timestamp = jdbcTemplate.queryForObject(sql, Timestamp.class, id);
            return Optional.of(timestamp.toLocalDateTime());
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
