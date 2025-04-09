package javaloginmodule.controller;

import javaloginmodule.model.UserDetailsResponse;
import javaloginmodule.model.UserRequest;
import javaloginmodule.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDetailsResponse> register(@RequestBody UserRequest request) {
        UserDetailsResponse response = authService.register(request);

        return null;
    }
}
