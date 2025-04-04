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

    @Autowired
    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public Optional<AuthResponse> register(AuthRequest authRequest) {
        String hashedPassword = passwordHasher.hash(authRequest.password());
        User candidateUser = new User(0, authRequest.username(), hashedPassword);
        Optional<User> createdUser = userRepository.save(candidateUser);
        return createdUser.map(user -> new AuthResponse(user.id(), user.username()));
    }

    public Optional<AuthResponse> authenticate(AuthRequest authRequest) {
        Optional<User> targetUser = validateUserCredentials(authRequest);
        return targetUser.map(user -> new AuthResponse(user.id(), user.username()));
    }

    public Optional<AuthResponse> update(AuthRequest authRequest, String newPassword) {
        Optional<User> targetUser = validateUserCredentials(authRequest);
        if (targetUser.isPresent()) {
            User userToUpdate = new User(targetUser.get().id(), targetUser.get().username(), passwordHasher.hash(newPassword));
            Optional<User> updatedUser = userRepository.update(userToUpdate);
            if (updatedUser.isPresent()) {
                return Optional.of(new AuthResponse(updatedUser.get().id(), updatedUser.get().username()));
            }
        }
        return Optional.empty();
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
        Optional<User> targetUser = validateUserCredentials(authRequest);
        if (targetUser.isPresent()) {
            return userRepository.delete(targetUser.get());
        }
        return false;
    }
}
