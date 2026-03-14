package kwh.PublicCookedFood.board.service.command;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class BoardSectionKeyValue {

    private static final int MAX_LENGTH = 50;

    private final String value;

    public BoardSectionKeyValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("게시판 키는 비어 있을 수 없습니다.");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("게시판 키는 50자 이하여야 합니다.");
        }
        this.value = value;
    }

    public static Optional<BoardSectionKeyValue> parse(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new BoardSectionKeyValue(rawValue));
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BoardSectionKeyValue)) {
            return false;
        }
        BoardSectionKeyValue that = (BoardSectionKeyValue) other;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
