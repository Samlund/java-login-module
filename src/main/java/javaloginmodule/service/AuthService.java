package javaloginmodule.service;

import javaloginmodule.security.PasswordHasher;
import javaloginmodule.model.AuthRequest;
import javaloginmodule.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private UserRepository userRepository;
    private PasswordHasher passwordHasher;

    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public void register(AuthRequest authRequest) {

    }
}
