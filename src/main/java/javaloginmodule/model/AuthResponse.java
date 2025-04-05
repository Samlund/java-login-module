package javaloginmodule.model;

public record AuthResponse(int id, String username, String token) {
    public static AuthResponse withoutToken(User user) {
        return new AuthResponse(user.id(), user.username(), null);
    }

    public static AuthResponse withToken(User user, String token) {
        return new AuthResponse(user.id(), user.username(), token);
    }
}
