package kwh.PublicCookedFood.board.dto.response;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
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

    private Long version;

    private SoftDeleteState state;

    private LocalDateTime regTime;

    private LocalDateTime updateTime;

    @Builder
    public BoardDetailResponse(Long id, String title, String contents, Long accountId, String accountName,
                               String accountProfileImageUrl,
                               Long sectionId, String sectionKey, String sectionName,
                               Long views, Long version, SoftDeleteState state,
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
        this.version = version;
        this.state = state;
        this.regTime = regTime;
        this.updateTime = updateTime;
    }

    public static BoardDetailResponse from(Board board) {
        BoardAuthorView author = BoardAuthorView.from(board.getAccount());
        BoardSectionView section = BoardSectionView.from(board.getSection());
        return BoardDetailResponse.builder()
                .id(board.getId())
                .title(board.getTitle())
                .contents(board.getContents())
                .accountId(author.accountId())
                .accountName(author.accountName())
                .accountProfileImageUrl(author.accountProfileImageUrl())
                .sectionId(section.sectionId())
                .sectionKey(section.sectionKey())
                .sectionName(section.sectionName())
                .version(board.getVersion())
                .state(board.getState())
                .regTime(board.getRegTime())
                .updateTime(board.getUpdateTime())
                .build();
    }

    public BoardAuthorView author() {
        if (accountId == null && accountName == null && accountProfileImageUrl == null) {
            return BoardAuthorView.anonymous();
        }
        return new BoardAuthorView(accountId, accountName, accountProfileImageUrl);
    }

    public BoardSectionView section() {
        if (sectionId == null && sectionKey == null && sectionName == null) {
            return BoardSectionView.unassigned();
        }
        return new BoardSectionView(sectionId, sectionKey, sectionName);
    }
}
