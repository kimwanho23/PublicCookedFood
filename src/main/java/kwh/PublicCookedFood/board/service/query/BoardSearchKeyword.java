package kwh.PublicCookedFood.board.service.query;

import java.util.Objects;
import java.util.Optional;

public final class BoardSearchKeyword {

    private final String value;

    public BoardSearchKeyword(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어는 비어 있을 수 없습니다.");
        }
        this.value = value.trim();
    }

    public static Optional<BoardSearchKeyword> from(String rawValue) {
        if (rawValue == null) {
            return Optional.empty();
        }
        String trimmed = rawValue.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new BoardSearchKeyword(trimmed));
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BoardSearchKeyword)) {
            return false;
        }
        BoardSearchKeyword other = (BoardSearchKeyword) obj;
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
