package javaloginmodule.model;

public record AuthResponse(UserResponse user, Token token) {
}
