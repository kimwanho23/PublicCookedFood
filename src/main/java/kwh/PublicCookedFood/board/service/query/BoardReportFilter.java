package kwh.PublicCookedFood.board.service.query;

import kwh.PublicCookedFood.board.domain.BoardReportStatus;

public enum BoardReportFilter {
    ALL,
    OPEN,
    RESOLVED,
    REJECTED;

    public static final String PARAM_ALL = "ALL";
    public static final String PARAM_OPEN = "OPEN";

    public static BoardReportFilter from(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return OPEN;
        }
        String normalized = rawValue.trim().toUpperCase();
        if (PARAM_ALL.equals(normalized)) {
            return ALL;
        }
        try {
            return BoardReportFilter.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return OPEN;
        }
    }

    public boolean includesAllStatuses() {
        return this == ALL;
    }

    public BoardReportStatus status() {
        return switch (this) {
            case OPEN -> BoardReportStatus.OPEN;
            case RESOLVED -> BoardReportStatus.RESOLVED;
            case REJECTED -> BoardReportStatus.REJECTED;
            case ALL -> throw new IllegalStateException("ALL filter does not map to a single status");
        };
    }

    public String paramValue() {
        return name();
    }
}
