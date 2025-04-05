package javaloginmodule.model;

public record AuthRequest(String username, String password, String token) {
    public static AuthRequest withToken(String username, String token) {
        return new AuthRequest(username, null, token);
    }

    public static AuthRequest withoutToken(String username, String password) {
        return new AuthRequest(username, password, null);
    }
}
