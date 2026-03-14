package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.application.query.view.BoardDetailActionsView;
import kwh.PublicCookedFood.board.application.query.view.BoardDetailCountersView;
import kwh.PublicCookedFood.board.application.query.view.BoardDetailPageView;
import kwh.PublicCookedFood.board.application.query.view.CommentNodeView;
import kwh.PublicCookedFood.board.application.query.view.CommentThreadPageView;
import kwh.PublicCookedFood.board.domain.BoardReportReason;
import kwh.PublicCookedFood.board.dto.response.BoardAuthorView;
import kwh.PublicCookedFood.board.dto.response.BoardDetailResponse;
import kwh.PublicCookedFood.board.dto.response.BoardSectionView;
import kwh.PublicCookedFood.board.service.BoardCounters;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class BoardDetailReadModelFactory {

    private static final List<BoardReportReason> REPORT_REASONS = List.of(BoardReportReason.values());

    public BoardDetailResponse applyCounters(BoardDetailResponse source, BoardCounters counters) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(counters, "counters");
        BoardAuthorView author = source.author();
        BoardSectionView section = source.section();
        return BoardDetailResponse.builder()
                .id(source.getId())
                .title(source.getTitle())
                .contents(source.getContents())
                .accountId(author.accountId())
                .accountName(author.accountName())
                .accountProfileImageUrl(author.accountProfileImageUrl())
                .sectionId(section.sectionId())
                .sectionKey(section.sectionKey())
                .sectionName(section.sectionName())
                .views(counters.views())
                .version(source.getVersion())
                .state(source.getState())
                .regTime(source.getRegTime())
                .updateTime(source.getUpdateTime())
                .build();
    }

    public BoardDetailPageView toViewData(BoardDetailResponse board,
                                          BoardCounters counters,
                                          long scraps,
                                          BoardInteractionState interactionState,
                                          Page<CommentNodeView> comments) {
        Objects.requireNonNull(counters, "counters");
        BoardDetailActionsView actions = BoardDetailActionsView.from(interactionState);
        return new BoardDetailPageView(
                Objects.requireNonNull(board, "board"),
                new BoardDetailCountersView(counters.likes(), scraps, counters.commentsCount()),
                actions,
                new CommentThreadPageView(
                        Objects.requireNonNull(board.getId(), "board.id"),
                        actions.currentAccountId(),
                        actions.boardInteractionBlocked(),
                        Objects.requireNonNull(comments, "comments")
                ),
                REPORT_REASONS
        );
    }
}
