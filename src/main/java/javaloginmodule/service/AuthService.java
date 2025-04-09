package javaloginmodule.service;

import javaloginmodule.model.*;
import javaloginmodule.security.PasswordHasher;
import javaloginmodule.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public Optional<UserDetailsResponse> register(UserRequest request) {
        String hashedPassword = passwordHasher.hash(request.password());
        User candidateUser = new User(0, request.username(), hashedPassword);
        Optional<User> savedUser = userRepository.save(candidateUser);
        return savedUser.flatMap(user -> userRepository.getUserCreationTimestamp(user.id())
                        .map(createdAt -> new UserDetailsResponse(user.id(), user.username(), createdAt)));
    }

    public Optional<AuthResponse> authenticate(UserRequest request) {
        Optional<User> targetUser = validateUserCredentials(request);
        return targetUser.map(user -> {
            Token token = new Token(tokenService.generateToken(user));
            UserResponse userResponse = new UserResponse(user.id(), user.username());
            return new AuthResponse(userResponse, token);
        });
    }

    public Optional<AuthResponse> updatePassword(Token token, String newPassword) {
        Optional<String> subject = tokenService.verifyToken(token.value());
        return subject.flatMap(extractedId -> parseUserId(extractedId))
                .flatMap(userId -> userRepository.fetchById(userId))
                .map(user -> new User(user.id(), user.username(), passwordHasher.hash(newPassword)))
                .flatMap(userToUpdate -> userRepository.update(userToUpdate))
                .map(updatedUser -> {
                    Token newToken = new Token(tokenService.generateToken(updatedUser));
                    UserResponse userResponse = new UserResponse(updatedUser.id(), updatedUser.username());
                    return new AuthResponse(userResponse, newToken);
                });
    }

    private Optional<User> validateUserCredentials(UserRequest request) {
        Optional<User> user = userRepository.fetchByUsername(request.username());
        return user.flatMap(targetUser -> passwordHasher.verify(request.password(), targetUser.passwordHash())
                ? Optional.of(targetUser)
                : Optional.empty());
    }

    public boolean delete(Token token) {
        Optional<String> subject = tokenService.verifyToken(token.value());
        return subject.flatMap(extractedId -> parseUserId(extractedId))
                .map(userId -> userRepository.delete(userId))
                .orElse(false);
    }

    private Optional<Integer> parseUserId(String subject) {
        try {
            return Optional.of(Integer.parseInt(subject));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
