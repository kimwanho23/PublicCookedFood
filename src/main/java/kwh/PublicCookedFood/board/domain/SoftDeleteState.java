package kwh.PublicCookedFood.board.domain;

public enum SoftDeleteState {
    ACTIVE("1"),
    DELETED("0");

    private final String dbValue;

    SoftDeleteState(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isDeleted() {
        return this == DELETED;
    }

    public static SoftDeleteState fromDbValue(String dbValue) {
        if ("0".equals(dbValue)) {
            return DELETED;
        }
        return ACTIVE;
    }
}
