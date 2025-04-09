package javaloginmodule.security;

import javaloginmodule.model.User;

import java.util.Optional;

public interface TokenService {
    String generateToken(User user);
    Optional<String> verifyToken(String token);
}
