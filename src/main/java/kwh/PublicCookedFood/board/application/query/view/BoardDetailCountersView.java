package kwh.PublicCookedFood.board.application.query.view;

import lombok.Getter;

@Getter
public final class BoardDetailCountersView {

    private final long likes;
    private final long scraps;
    private final long commentsCount;

    public BoardDetailCountersView(long likes,
                                   long scraps,
                                   long commentsCount) {
        if (likes < 0L) {
            throw new IllegalArgumentException("likes must not be negative");
        }
        if (scraps < 0L) {
            throw new IllegalArgumentException("scraps must not be negative");
        }
        if (commentsCount < 0L) {
            throw new IllegalArgumentException("commentsCount must not be negative");
        }
        this.likes = likes;
        this.scraps = scraps;
        this.commentsCount = commentsCount;
    }

    public long likes() {
        return likes;
    }

    public long scraps() {
        return scraps;
    }

    public long commentsCount() {
        return commentsCount;
    }
}
