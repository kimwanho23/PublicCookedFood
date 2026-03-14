package kwh.PublicCookedFood.board.service.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class BoardSectionReorderCommand {

    private final List<Long> sectionIds;

    public BoardSectionReorderCommand(List<Long> sectionIds) {
        this.sectionIds = Collections.unmodifiableList(
                new ArrayList<Long>(Objects.requireNonNull(sectionIds, "게시판 탭 순서 정보가 비어 있습니다."))
        );
    }

    public static BoardSectionReorderCommand of(List<Long> sectionIds) {
        return new BoardSectionReorderCommand(sectionIds);
    }

    public List<Long> sectionIds() {
        return sectionIds;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BoardSectionReorderCommand)) {
            return false;
        }
        BoardSectionReorderCommand that = (BoardSectionReorderCommand) other;
        return Objects.equals(sectionIds, that.sectionIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sectionIds);
    }
}
