package javaloginmodule.model;

import java.time.LocalDateTime;

public record UserDetailsResponse(int id, String username, LocalDateTime createdAt) {
}
