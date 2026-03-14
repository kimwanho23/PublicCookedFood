package kwh.PublicCookedFood.board.service.command;

import java.util.Objects;

public final class BoardSectionCreateCommand {

    private final BoardSectionKeyValue sectionKey;
    private final BoardSectionNameValue sectionName;
    private final Integer displayOrder;

    public BoardSectionCreateCommand(BoardSectionKeyValue sectionKey,
                                     BoardSectionNameValue sectionName,
                                     Integer displayOrder) {
        if (sectionName == null) {
            throw new IllegalArgumentException("게시판 탭 이름은 필수입니다.");
        }
        this.sectionKey = sectionKey;
        this.sectionName = sectionName;
        this.displayOrder = displayOrder;
    }

    public static BoardSectionCreateCommand of(String sectionKey,
                                               String sectionName,
                                               Integer displayOrder) {
        return new BoardSectionCreateCommand(
                BoardSectionKeyValue.parse(sectionKey).orElse(null),
                BoardSectionNameValue.from(sectionName),
                displayOrder
        );
    }

    public BoardSectionKeyValue sectionKey() {
        return sectionKey;
    }

    public BoardSectionNameValue sectionName() {
        return sectionName;
    }

    public Integer displayOrder() {
        return displayOrder;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BoardSectionCreateCommand)) {
            return false;
        }
        BoardSectionCreateCommand that = (BoardSectionCreateCommand) other;
        return Objects.equals(sectionKey, that.sectionKey)
                && Objects.equals(sectionName, that.sectionName)
                && Objects.equals(displayOrder, that.displayOrder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sectionKey, sectionName, displayOrder);
    }
}
