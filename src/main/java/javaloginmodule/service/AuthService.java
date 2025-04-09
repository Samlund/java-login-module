package javaloginmodule.service;

import javaloginmodule.exceptions.BadRequestException;
import javaloginmodule.exceptions.InvalidCredentialsException;
import javaloginmodule.exceptions.UnauthorizedAccessException;
import javaloginmodule.exceptions.UserNotFoundException;
import javaloginmodule.model.*;
import javaloginmodule.security.PasswordHasher;
import javaloginmodule.repository.UserRepository;
import javaloginmodule.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;

    @Autowired
    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
    }

    public UserDetailsResponse register(UserRequest request) {
        String username = request.username();
        String password = request.password();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BadRequestException("Username or password cannot be null");
        }

        String hashedPassword = passwordHasher.hash(password);
        User savedUser = userRepository.save(new User(0, username, hashedPassword));
        LocalDateTime createdAt = userRepository.getUserCreationTimestamp(savedUser.id())
                .orElseThrow(() -> new IllegalStateException("User was created but creation timestamp is missing"));

        return new UserDetailsResponse(savedUser.id(), savedUser.username(), createdAt);
    }

    public AuthResponse authenticate(UserRequest request) {
        User user = validateUserCredentials(request);
        Token token = new Token(tokenService.generateToken(user));
        UserResponse response = new UserResponse(user.id(), user.username());
        return new AuthResponse(response, token);
    }

    public AuthResponse updatePassword(Token token, String newPassword) {
        String userId = tokenService.verifyToken(token.value())
                .orElseThrow(() -> new UnauthorizedAccessException("Invalid or expired token"));

        int id = parseUserId(userId);
        User user = userRepository.fetchById(id).orElseThrow(() -> new UserNotFoundException(id));
        User updatedUser = userRepository.update(new User(user.id(), user.username(), passwordHasher.hash(newPassword)));

        UserResponse response = new UserResponse(updatedUser.id(), updatedUser.username());
        Token newToken = new Token(tokenService.generateToken(updatedUser));
        return new AuthResponse(response, newToken);
    }

    private User validateUserCredentials(UserRequest request) {
        User targetUser = userRepository.fetchByUsername(request.username());
        if (passwordHasher.verify(request.password(), targetUser.passwordHash())) {
            return targetUser;
        } else {
            throw new InvalidCredentialsException("Invalid credentials");
        }
    }

    public void delete(Token token) {
        String userId = tokenService.verifyToken(token.value())
                .orElseThrow(() -> new UnauthorizedAccessException("Invalid or expired token"));

        int id = parseUserId(userId);
        try {
            userRepository.delete(id);
        } catch (UserNotFoundException e) {
            throw new UnauthorizedAccessException("Invalid or expired token");
        }
    }

    private int parseUserId(String subject) {
        try {
            return Integer.parseInt(subject);
        } catch (NumberFormatException e) {
            throw new UnauthorizedAccessException("Invalid user ID in token");
        }
    }
}
