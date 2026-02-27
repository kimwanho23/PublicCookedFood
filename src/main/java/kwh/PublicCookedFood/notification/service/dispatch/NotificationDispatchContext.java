package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardReport;
import kwh.PublicCookedFood.board.domain.Comments;

public record NotificationDispatchContext(
        Comments comment,
        Board board,
        BoardReport report
) {

    public static NotificationDispatchContext forNewComment(Comments comment) {
        return new NotificationDispatchContext(comment, null, null);
    }

    public static NotificationDispatchContext forBoardCreated(Board board) {
        return new NotificationDispatchContext(null, board, null);
    }

    public static NotificationDispatchContext forReportProcessed(BoardReport report) {
        return new NotificationDispatchContext(null, null, report);
    }
}

