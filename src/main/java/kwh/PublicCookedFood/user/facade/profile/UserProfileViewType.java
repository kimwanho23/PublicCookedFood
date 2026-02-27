package kwh.PublicCookedFood.user.facade.profile;

import java.util.Locale;

public enum UserProfileViewType {
    BOARDS("boards"),
    COMMENTS("comments"),
    SCRAPS("scraps");

    private final String key;

    UserProfileViewType(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static UserProfileViewType from(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return BOARDS;
        }

        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "comments" -> COMMENTS;
            case "scraps" -> SCRAPS;
            default -> BOARDS;
        };
    }
}
