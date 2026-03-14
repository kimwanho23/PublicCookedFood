package kwh.PublicCookedFood.board.service.command;

import kwh.PublicCookedFood.board.domain.BoardReportReason;

import java.util.Objects;

public final class BoardReportCreateCommand {

    private final long boardId;
    private final long reporterId;
    private final BoardReportReason reason;
    private final String details;

    public BoardReportCreateCommand(long boardId,
                                    long reporterId,
                                    BoardReportReason reason,
                                    String details) {
        if (boardId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 게시글입니다.");
        }
        if (reporterId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 사용자입니다.");
        }
        if (reason == null) {
            throw new IllegalArgumentException("신고 사유를 선택해주세요.");
        }
        this.boardId = boardId;
        this.reporterId = reporterId;
        this.reason = reason;
        this.details = details;
    }

    public static BoardReportCreateCommand of(Long boardId,
                                              Long reporterId,
                                              BoardReportReason reason,
                                              String details) {
        return new BoardReportCreateCommand(
                Objects.requireNonNull(boardId, "유효하지 않은 게시글입니다."),
                Objects.requireNonNull(reporterId, "유효하지 않은 사용자입니다."),
                reason,
                details
        );
    }

    public long boardId() {
        return boardId;
    }

    public long reporterId() {
        return reporterId;
    }

    public BoardReportReason reason() {
        return reason;
    }

    public String details() {
        return details;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BoardReportCreateCommand)) {
            return false;
        }
        BoardReportCreateCommand that = (BoardReportCreateCommand) other;
        return boardId == that.boardId
                && reporterId == that.reporterId
                && reason == that.reason
                && Objects.equals(details, that.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(boardId, reporterId, reason, details);
    }
}
