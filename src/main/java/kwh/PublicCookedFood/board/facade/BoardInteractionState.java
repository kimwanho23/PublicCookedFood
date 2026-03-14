package kwh.PublicCookedFood.board.facade;

import java.util.Objects;
import java.util.Optional;

public final class BoardInteractionState {

    private final BoardViewer viewer;
    private final boolean myLike;
    private final boolean myScrap;
    private final boolean myReport;
    private final boolean myBlockedAuthor;
    private final boolean boardInteractionBlocked;

    public BoardInteractionState(BoardViewer viewer,
                                 boolean myLike,
                                 boolean myScrap,
                                 boolean myReport,
                                 boolean myBlockedAuthor,
                                 boolean boardInteractionBlocked) {
        BoardViewer safeViewer = Objects.requireNonNull(viewer, "viewer");
        this.viewer = safeViewer;
        if (!safeViewer.isAuthenticated()) {
            this.myLike = false;
            this.myScrap = false;
            this.myReport = false;
            this.myBlockedAuthor = false;
            this.boardInteractionBlocked = false;
            return;
        }
        this.myLike = myLike;
        this.myScrap = myScrap;
        this.myReport = myReport;
        this.myBlockedAuthor = myBlockedAuthor;
        this.boardInteractionBlocked = myBlockedAuthor;
    }

    public static BoardInteractionState anonymous() {
        return new BoardInteractionState(BoardViewer.anonymous(), false, false, false, false, false);
    }

    public static BoardInteractionState authenticated(long currentAccountId,
                                                      boolean myLike,
                                                      boolean myScrap,
                                                      boolean myReport,
                                                      boolean myBlockedAuthor) {
        return authenticated(BoardViewer.authenticated(currentAccountId), myLike, myScrap, myReport, myBlockedAuthor);
    }

    public static BoardInteractionState authenticated(BoardViewer.Authenticated viewer,
                                                      boolean myLike,
                                                      boolean myScrap,
                                                      boolean myReport,
                                                      boolean myBlockedAuthor) {
        return new BoardInteractionState(
                viewer,
                myLike,
                myScrap,
                myReport,
                myBlockedAuthor,
                myBlockedAuthor
        );
    }

    public BoardViewer viewer() {
        return viewer;
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

    public Optional<Long> maybeCurrentAccountId() {
        return viewer.maybeAccountId();
    }

    public boolean authenticated() {
        return viewer.isAuthenticated();
    }
}
