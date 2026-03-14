package kwh.PublicCookedFood.board.service.support;

import lombok.Getter;

import java.util.Objects;

@Getter
public final class SanitizedBoardContent {

    private final String title;
    private final String contents;

    public SanitizedBoardContent(String title, String contents) {
        this.title = title;
        this.contents = contents;
    }

    public String title() {
        return title;
    }

    public String contents() {
        return contents;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SanitizedBoardContent other)) {
            return false;
        }
        return Objects.equals(title, other.title)
                && Objects.equals(contents, other.contents);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, contents);
    }
}
