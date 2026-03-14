package kwh.PublicCookedFood.board.application.query.view;

import kwh.PublicCookedFood.board.dto.response.CommentAuthorView;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public final class CommentNodeView {

    private final Long id;
    private final CommentAuthorView author;
    private final Long boardId;
    private final String contents;
    private final Long parentId;
    private final SoftDeleteState state;
    private final List<CommentNodeView> replies;
    private final LocalDateTime regTime;
    private final LocalDateTime updateTime;

    public CommentNodeView(Long id,
                           CommentAuthorView author,
                           Long boardId,
                           String contents,
                           Long parentId,
                           SoftDeleteState state,
                           List<CommentNodeView> replies,
                           LocalDateTime regTime,
                           LocalDateTime updateTime) {
        this.id = id;
        this.author = author == null ? CommentAuthorView.anonymous() : author;
        this.boardId = boardId;
        this.contents = contents;
        this.parentId = parentId;
        this.state = state;
        this.replies = unmodifiableReplies(replies);
        this.regTime = regTime;
        this.updateTime = updateTime;
    }

    public Long id() {
        return id;
    }

    public Long getAccountId() {
        return author.accountId();
    }

    public CommentAuthorView author() {
        return author;
    }

    public Long boardId() {
        return boardId;
    }

    public String contents() {
        return contents;
    }

    public Long parentId() {
        return parentId;
    }

    public SoftDeleteState state() {
        return state;
    }

    public List<CommentNodeView> replies() {
        return replies;
    }

    public LocalDateTime regTime() {
        return regTime;
    }

    public LocalDateTime updateTime() {
        return updateTime;
    }

    public String getName() {
        return author.name();
    }

    public String getProfileImageUrl() {
        return author.profileImageUrl();
    }

    public boolean isActive() {
        return state != null && state.isActive();
    }

    public boolean isDeleted() {
        return state != null && state.isDeleted();
    }

    public boolean areAllRepliesDeleted() {
        if (!isDeleted()) {
            return false;
        }
        return replies.stream().allMatch(reply -> reply.isDeleted() && reply.areAllRepliesDeleted());
    }

    private static List<CommentNodeView> unmodifiableReplies(List<CommentNodeView> replies) {
        if (replies == null || replies.isEmpty()) {
            return Collections.emptyList();
        }
        return List.copyOf(replies);
    }
}
