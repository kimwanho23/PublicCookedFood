package kwh.PublicCookedFood.board.service.command;

import java.util.Objects;

public final class BoardSectionUpdateCommand {

    private final long sectionId;
    private final BoardSectionNameValue sectionName;
    private final Integer displayOrder;
    private final Boolean active;

    public BoardSectionUpdateCommand(long sectionId,
                                     BoardSectionNameValue sectionName,
                                     Integer displayOrder,
                                     Boolean active) {
        if (sectionId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 게시판 탭입니다.");
        }
        if (sectionName == null) {
            throw new IllegalArgumentException("게시판 탭 이름은 필수입니다.");
        }
        this.sectionId = sectionId;
        this.sectionName = sectionName;
        this.displayOrder = displayOrder;
        this.active = active;
    }

    public static BoardSectionUpdateCommand of(Long sectionId,
                                               String sectionName,
                                               Integer displayOrder,
                                               Boolean active) {
        return new BoardSectionUpdateCommand(
                Objects.requireNonNull(sectionId, "유효하지 않은 게시판 탭입니다."),
                BoardSectionNameValue.from(sectionName),
                displayOrder,
                active
        );
    }

    public long sectionId() {
        return sectionId;
    }

    public BoardSectionNameValue sectionName() {
        return sectionName;
    }

    public Integer displayOrder() {
        return displayOrder;
    }

    public Boolean active() {
        return active;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BoardSectionUpdateCommand)) {
            return false;
        }
        BoardSectionUpdateCommand that = (BoardSectionUpdateCommand) other;
        return sectionId == that.sectionId
                && Objects.equals(sectionName, that.sectionName)
                && Objects.equals(displayOrder, that.displayOrder)
                && Objects.equals(active, that.active);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sectionId, sectionName, displayOrder, active);
    }
}
