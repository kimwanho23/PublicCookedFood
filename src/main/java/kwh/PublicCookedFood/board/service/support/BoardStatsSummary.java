package kwh.PublicCookedFood.board.service.support;

import lombok.Getter;

import java.util.Objects;

@Getter
public final class BoardStatsSummary {

    public static final BoardStatsSummary ZERO = new BoardStatsSummary(0L, 0L, 0L);

    private final long views;
    private final long likes;
    private final long commentsCount;

    public BoardStatsSummary(long views, long likes, long commentsCount) {
        if (views < 0L) {
            throw new IllegalArgumentException("조회수는 음수일 수 없습니다.");
        }
        if (likes < 0L) {
            throw new IllegalArgumentException("좋아요 수는 음수일 수 없습니다.");
        }
        if (commentsCount < 0L) {
            throw new IllegalArgumentException("댓글 수는 음수일 수 없습니다.");
        }
        this.views = views;
        this.likes = likes;
        this.commentsCount = commentsCount;
    }

    public long views() {
        return views;
    }

    public long likes() {
        return likes;
    }

    public long commentsCount() {
        return commentsCount;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BoardStatsSummary other)) {
            return false;
        }
        return views == other.views
                && likes == other.likes
                && commentsCount == other.commentsCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(views, likes, commentsCount);
    }
}
