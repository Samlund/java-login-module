package javaloginmodule.service;

import javaloginmodule.model.AuthResponse;
import javaloginmodule.model.User;
import javaloginmodule.security.PasswordHasher;
import javaloginmodule.model.AuthRequest;
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

    public Optional<AuthResponse> register(AuthRequest authRequest) {
        String hashedPassword = passwordHasher.hash(authRequest.password());
        User candidateUser = new User(0, authRequest.username(), hashedPassword);
        Optional<User> createdUser = userRepository.save(candidateUser);
        return createdUser.map(user -> AuthResponse.withoutToken(createdUser.get()));
    }

    public Optional<AuthResponse> authenticate(AuthRequest authRequest) {
        Optional<User> targetUser = validateUserCredentials(authRequest);
        return targetUser.map(user -> {
            String token = tokenService.generateToken(targetUser.get());
            return AuthResponse.withToken(targetUser.get(), token);
        });
    }

    public Optional<AuthResponse> update(AuthRequest authRequest, String newPassword) {
        return tokenService.verifyToken(authRequest.token())
                .flatMap(subject -> parseUserId(subject))
                .flatMap(userId -> userRepository.fetchById(userId))
                .map(user -> new User(user.id(), user.username(), passwordHasher.hash(newPassword)))
                .flatMap(userToUpdate -> userRepository.update(userToUpdate))
                .map(updatedUser -> {
                    String token = tokenService.generateToken(updatedUser);
                    return AuthResponse.withToken(updatedUser, token);
                });
    }

    private Optional<User> validateUserCredentials(AuthRequest authRequest) {
        Optional<User> user = userRepository.fetchByUsername(authRequest.username());
        if (user.isEmpty()) {
            return Optional.empty();
        }
        User targetUser = user.get();
        if (passwordHasher.verify(authRequest.password(), targetUser.passwordHash())) {
            return Optional.of(targetUser);
        }
        return Optional.empty();
    }

    public boolean delete(AuthRequest authRequest) {
        return tokenService.verifyToken(authRequest.token())
                .flatMap(subject -> parseUserId(subject))
                .flatMap(userId -> userRepository.fetchById(userId))
                .map(user -> new User(user.id(), user.username(), user.passwordHash()))
                .map(userToDelete -> userRepository.delete(userToDelete))
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
