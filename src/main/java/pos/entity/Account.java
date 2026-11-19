package pos.entity;

import java.util.Set;

public record Account(Long id, String username, String passwordHash, String fullName,
                      String roleCode, String status, Set<String> permissions) {
    public Account {
        permissions = Set.copyOf(permissions);
    }

    @Override
    public String toString() {
        return "Account[id=" + id + ", username=" + username + ", status=" + status + "]";
    }
}
