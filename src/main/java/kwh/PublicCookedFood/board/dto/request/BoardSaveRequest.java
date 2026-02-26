package kwh.PublicCookedFood.board.dto.request;

import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BoardSaveRequest {

    private Long id;

    private String title;

    private String contents;

    private Long userId;

    private Long sectionId;

    private Long views;

    private Long likesCount;

    private Long commentsCount;

    private SoftDeleteState state;

    @Builder
    public BoardSaveRequest(Long id, String title, String contents, Long userId, Long sectionId, Long views,
                            Long likesCount, Long commentsCount, SoftDeleteState state) {
        this.id = id;
        this.title = title;
        this.contents = contents;
        this.userId = userId;
        this.sectionId = sectionId;
        this.views = views;
        this.likesCount = likesCount;
        this.commentsCount = commentsCount;
        this.state = state;
    }
}
