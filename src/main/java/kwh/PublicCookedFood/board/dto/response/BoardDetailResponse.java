package kwh.PublicCookedFood.board.dto.response;

import kwh.PublicCookedFood.board.domain.Board;
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

    private Long accountId;

    private String accountName;

    private String accountProfileImageUrl;

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
    public BoardDetailResponse(Long id, String title, String contents, Long accountId, String accountName,
                               String accountProfileImageUrl,
                               Long sectionId, String sectionKey, String sectionName,
                               Long views, Long likesCount, Long commentsCount, SoftDeleteState state,
                               LocalDateTime regTime, LocalDateTime updateTime) {
        this.id = id;
        this.title = title;
        this.contents = contents;
        this.accountId = accountId;
        this.accountName = accountName;
        this.accountProfileImageUrl = accountProfileImageUrl;
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

    public static BoardDetailResponse from(Board board) {
        return BoardDetailResponse.builder()
                .id(board.getId())
                .title(board.getTitle())
                .contents(board.getContents())
                .accountId(board.getAccount() == null ? null : board.getAccount().getId())
                .accountName(board.getAccount() == null ? null : board.getAccount().getName())
                .accountProfileImageUrl(board.getAccount() == null ? null : board.getAccount().getProfileImageUrl())
                .sectionId(board.getSection() == null ? null : board.getSection().getId())
                .sectionKey(board.getSection() == null ? null : board.getSection().getSectionKey())
                .sectionName(board.getSection() == null ? null : board.getSection().getSectionName())
                .views(board.getViews())
                .likesCount(board.getLikeCount())
                .commentsCount(board.getCommentCount())
                .state(board.getState())
                .regTime(board.getRegTime())
                .updateTime(board.getUpdateTime())
                .build();
    }
}
