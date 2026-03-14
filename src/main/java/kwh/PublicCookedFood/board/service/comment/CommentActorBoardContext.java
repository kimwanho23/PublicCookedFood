package kwh.PublicCookedFood.board.service.comment;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.board.domain.Board;

import java.util.Objects;

public final class CommentActorBoardContext {

    private final Account actor;
    private final Board board;

    public CommentActorBoardContext(Account actor, Board board) {
        this.actor = Objects.requireNonNull(actor, "actor");
        this.board = Objects.requireNonNull(board, "board");
    }

    public static CommentActorBoardContext of(Account actor, Board board) {
        return new CommentActorBoardContext(actor, board);
    }

    public Account actor() {
        return actor;
    }

    public Board board() {
        return board;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CommentActorBoardContext)) {
            return false;
        }
        CommentActorBoardContext that = (CommentActorBoardContext) other;
        return Objects.equals(actor, that.actor) && Objects.equals(board, that.board);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actor, board);
    }
}
