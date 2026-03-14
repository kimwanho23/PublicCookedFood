package kwh.PublicCookedFood.board.service.image;

import kwh.PublicCookedFood.board.domain.Board;

import java.util.Objects;

public final class BoardImageSyncCommand {

    private final Board board;
    private final String htmlContent;

    public BoardImageSyncCommand(Board board, String htmlContent) {
        this.board = board;
        this.htmlContent = htmlContent;
    }

    public static BoardImageSyncCommand from(Board board) {
        return new BoardImageSyncCommand(board, board == null ? null : board.getContents());
    }

    public static BoardImageSyncCommand of(Board board, String htmlContent) {
        return new BoardImageSyncCommand(board, htmlContent);
    }

    public Board board() {
        return board;
    }

    public String htmlContent() {
        return htmlContent;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BoardImageSyncCommand)) {
            return false;
        }
        BoardImageSyncCommand that = (BoardImageSyncCommand) other;
        return Objects.equals(board, that.board) && Objects.equals(htmlContent, that.htmlContent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(board, htmlContent);
    }
}
