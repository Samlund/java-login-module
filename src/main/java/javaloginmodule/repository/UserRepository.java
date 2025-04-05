package javaloginmodule.repository;

import javaloginmodule.model.User;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> save(User user) {
        String sql = "INSERT INTO users (username, password) VALUES (?,?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
                ps.setString(1, user.username());
                ps.setString(2, user.passwordHash());
                return ps;
            }, keyHolder);

            int id = keyHolder.getKey().intValue();
            return Optional.of(new User(id, user.username(), user.passwordHash()));
        } catch (DataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<User> update(User user) {
        String sql = "UPDATE users SET password = ? WHERE username = ?";

        try {
            int rowsAffected = jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql);
                ps.setString(1, user.passwordHash());
                ps.setString(2, user.username());
                return ps;
            });

            if (rowsAffected == 0) {
                return Optional.empty();
            }

            return Optional.of(user);
        } catch (DataAccessException e) {
            return Optional.empty();
        }
    }

    public boolean delete(User user) {
        String sql = "DELETE FROM users WHERE id = ?";
        try {
            int rowsAffected = jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql);
                ps.setInt(1, user.id());
                return ps;
            });

            return rowsAffected != 0;
        } catch (DataAccessException e) {
            return false;
        }
    }

    public Optional<User> fetchByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try {
            User user = jdbcTemplate.queryForObject(
                    sql,
                    (rs, rowNum) -> new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password")
                    ),
                    username
            );
            return Optional.of(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
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
}
