package kwh.PublicCookedFood.board.service.command;

import java.util.Objects;

public final class BoardSectionNameValue {

    private static final int MAX_LENGTH = 100;

    private final String value;

    public BoardSectionNameValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("게시판 이름은 비어 있을 수 없습니다.");
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("게시판 탭 이름은 100자 이하여야 합니다.");
        }
        this.value = value;
    }

    public static BoardSectionNameValue from(String rawValue) {
        return new BoardSectionNameValue(rawValue);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BoardSectionNameValue)) {
            return false;
        }
        BoardSectionNameValue that = (BoardSectionNameValue) other;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
