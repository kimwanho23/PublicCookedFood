package kwh.PublicCookedFood.board.application.query.view;

import kwh.PublicCookedFood.board.facade.BoardInteractionState;
import lombok.Getter;

import java.util.Objects;

@Getter
public final class BoardDetailActionsView {

    private final Long currentAccountId;
    private final boolean myLike;
    private final boolean myScrap;
    private final boolean myReport;
    private final boolean myBlockedAuthor;
    private final boolean boardInteractionBlocked;

    public BoardDetailActionsView(Long currentAccountId,
                                  boolean myLike,
                                  boolean myScrap,
                                  boolean myReport,
                                  boolean myBlockedAuthor,
                                  boolean boardInteractionBlocked) {
        this.currentAccountId = currentAccountId;
        this.myLike = myLike;
        this.myScrap = myScrap;
        this.myReport = myReport;
        this.myBlockedAuthor = myBlockedAuthor;
        this.boardInteractionBlocked = boardInteractionBlocked;
    }

    public static BoardDetailActionsView from(BoardInteractionState interactionState) {
        Objects.requireNonNull(interactionState, "interactionState");
        return new BoardDetailActionsView(
                interactionState.maybeCurrentAccountId().orElse(null),
                interactionState.myLike(),
                interactionState.myScrap(),
                interactionState.myReport(),
                interactionState.myBlockedAuthor(),
                interactionState.boardInteractionBlocked()
        );
    }

    public Long currentAccountId() {
        return currentAccountId;
    }

    public boolean myLike() {
        return myLike;
    }

    public boolean myScrap() {
        return myScrap;
    }

    public boolean myReport() {
        return myReport;
    }

    public boolean myBlockedAuthor() {
        return myBlockedAuthor;
    }

    public boolean boardInteractionBlocked() {
        return boardInteractionBlocked;
    }

}
