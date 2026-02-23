package kwh.PublicCookedFood.board.dto.response;

import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class BoardDetailResponse {

    private Long id;

    private String title;

    private String contents;

    private Long userId;

    private String userName;

    private Long sectionId;

    private String sectionKey;

    private String sectionName;

    private Long views;

    private Long likesCount;

    private Long commentsCount;

    private SoftDeleteState state;

    private LocalDateTime regTime;

    private LocalDateTime updateTime;

    @Builder
    public BoardDetailResponse(Long id, String title, String contents, Long userId, String userName,
                               Long sectionId, String sectionKey, String sectionName,
                               Long views, Long likesCount, Long commentsCount, SoftDeleteState state,
                               LocalDateTime regTime, LocalDateTime updateTime) {
        this.id = id;
        this.title = title;
        this.contents = contents;
        this.userId = userId;
        this.userName = userName;
        this.sectionId = sectionId;
        this.sectionKey = sectionKey;
        this.sectionName = sectionName;
        this.views = views;
        this.likesCount = likesCount;
        this.commentsCount = commentsCount;
        this.state = state;
        this.regTime = regTime;
        this.updateTime = updateTime;
    }
}
