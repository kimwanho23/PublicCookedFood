package kwh.PublicCookedFood.board.application.query.view;

import kwh.PublicCookedFood.board.domain.BoardReportReason;
import kwh.PublicCookedFood.board.dto.response.BoardDetailResponse;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Getter
public final class BoardDetailPageView {

    private final BoardDetailResponse board;
    private final BoardDetailCountersView counters;
    private final BoardDetailActionsView actions;
    private final CommentThreadPageView commentThread;
    private final List<BoardReportReason> reportReasons;

    public BoardDetailPageView(BoardDetailResponse board,
                               BoardDetailCountersView counters,
                               BoardDetailActionsView actions,
                               CommentThreadPageView commentThread,
                               List<BoardReportReason> reportReasons) {
        this.board = Objects.requireNonNull(board, "board");
        this.counters = Objects.requireNonNull(counters, "counters");
        this.actions = Objects.requireNonNull(actions, "actions");
        this.commentThread = Objects.requireNonNull(commentThread, "commentThread");
        this.reportReasons = unmodifiableReportReasons(reportReasons);
    }

    public Long boardId() {
        return board.getId();
    }

    public Long getBoardId() {
        return boardId();
    }

    public Page<CommentNodeView> comments() {
        return commentThread.comments();
    }

    public Page<CommentNodeView> getComments() {
        return comments();
    }

    public BoardDetailResponse board() {
        return board;
    }

    public BoardDetailCountersView counters() {
        return counters;
    }

    public BoardDetailActionsView actions() {
        return actions;
    }

    public CommentThreadPageView commentThread() {
        return commentThread;
    }

    public List<BoardReportReason> reportReasons() {
        return reportReasons;
    }

    private static List<BoardReportReason> unmodifiableReportReasons(List<BoardReportReason> reportReasons) {
        if (reportReasons == null || reportReasons.isEmpty()) {
            return Collections.emptyList();
        }
        return List.copyOf(reportReasons);
    }
}
