package kwh.PublicCookedFood.board.service.query;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class BoardSectionKey {

    private final String value;

    public BoardSectionKey(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("게시판 탭 키는 비어 있을 수 없습니다.");
        }
        this.value = value.trim().toLowerCase(Locale.ROOT);
    }

    public static Optional<BoardSectionKey> from(String rawValue) {
        if (rawValue == null) {
            return Optional.empty();
        }
        String trimmed = rawValue.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new BoardSectionKey(trimmed));
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BoardSectionKey)) {
            return false;
        }
        BoardSectionKey other = (BoardSectionKey) obj;
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
