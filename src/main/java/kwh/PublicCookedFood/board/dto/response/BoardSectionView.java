package kwh.PublicCookedFood.board.dto.response;

import kwh.PublicCookedFood.board.domain.BoardSection;
import lombok.Getter;

import java.util.Objects;

@Getter
public final class BoardSectionView {

    private static final BoardSectionView UNASSIGNED = new BoardSectionView(null, null, null);

    private final Long sectionId;
    private final String sectionKey;
    private final String sectionName;

    public BoardSectionView(Long sectionId, String sectionKey, String sectionName) {
        this.sectionId = sectionId;
        this.sectionKey = sectionKey;
        this.sectionName = sectionName;
    }

    public static BoardSectionView unassigned() {
        return UNASSIGNED;
    }

    public static BoardSectionView from(BoardSection section) {
        if (section == null) {
            return unassigned();
        }
        return new BoardSectionView(
                section.getId(),
                section.getSectionKey(),
                section.getSectionName()
        );
    }

    public Long sectionId() {
        return sectionId;
    }

    public String sectionKey() {
        return sectionKey;
    }

    public String sectionName() {
        return sectionName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BoardSectionView)) {
            return false;
        }
        BoardSectionView other = (BoardSectionView) obj;
        return Objects.equals(sectionId, other.sectionId)
                && Objects.equals(sectionKey, other.sectionKey)
                && Objects.equals(sectionName, other.sectionName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sectionId, sectionKey, sectionName);
    }
}
