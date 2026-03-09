package kwh.PublicCookedFood.userrecipe.domain;

public enum UserRecipeIngredientGroup {
    MAIN("주재료", 0),
    SUB("부재료", 1),
    SEASONING("양념", 2);

    private final String displayName;
    private final int displayOrder;

    UserRecipeIngredientGroup(String displayName, int displayOrder) {
        this.displayName = displayName;
        this.displayOrder = displayOrder;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public static UserRecipeIngredientGroup from(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("재료 구분은 필수입니다.");
        }
        String normalized = rawValue.trim().toUpperCase();
        return switch (normalized) {
            case "MAIN", "주재료" -> MAIN;
            case "SUB", "부재료" -> SUB;
            case "SEASONING", "양념" -> SEASONING;
            default -> throw new IllegalArgumentException("지원하지 않는 재료 구분입니다.");
        };
    }
}
