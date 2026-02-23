package kwh.PublicCookedFood.board.dto.response;

import kwh.PublicCookedFood.board.domain.BoardSection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BoardSectionResponse {

    private Long id;
    private String sectionKey;
    private String sectionName;
    private Integer displayOrder;
    private Boolean active;

    @Builder
    public BoardSectionResponse(Long id, String sectionKey, String sectionName, Integer displayOrder, Boolean active) {
        this.id = id;
        this.sectionKey = sectionKey;
        this.sectionName = sectionName;
        this.displayOrder = displayOrder;
        this.active = active;
    }

    public static BoardSectionResponse from(BoardSection section) {
        return BoardSectionResponse.builder()
                .id(section.getId())
                .sectionKey(section.getSectionKey())
                .sectionName(section.getSectionName())
                .displayOrder(section.getDisplayOrder())
                .active(section.getActive())
                .build();
    }
}
