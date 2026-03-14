package kwh.PublicCookedFood.board.service.query;

import org.springframework.data.domain.Sort;

public enum BoardListOrder {
    RECENT("recent"),
    VIEWS("views"),
    LIKES("likes"),
    COMMENTS("comments");

    private final String paramValue;

    BoardListOrder(String paramValue) {
        this.paramValue = paramValue;
    }

    public static BoardListOrder from(String value) {
        if (value == null || value.isBlank()) {
            return RECENT;
        }
        return switch (value.trim().toLowerCase()) {
            case "views" -> VIEWS;
            case "likes" -> LIKES;
            case "comments" -> COMMENTS;
            default -> RECENT;
        };
    }

    public String paramValue() {
        return paramValue;
    }

    public boolean usesStatsOrdering() {
        return switch (this) {
            case VIEWS, LIKES, COMMENTS -> true;
            case RECENT -> false;
        };
    }

    public Sort sort() {
        return switch (this) {
            case VIEWS, LIKES, COMMENTS -> Sort.unsorted();
            case RECENT -> Sort.by(Sort.Order.desc("regTime"));
        };
    }
}
