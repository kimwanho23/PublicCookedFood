package kwh.PublicCookedFood.board.dto.response;

import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CommentResponse {

    private Long id;

    private Long userId;

    private String name;

    private String profileImageUrl;

    private Long boardId;

    private String contents;

    private Long parentId;

    private SoftDeleteState state;

    private List<CommentResponse> replies = new ArrayList<>();

    private boolean areAllRepliesDeleted;

    private LocalDateTime regTime;

    private LocalDateTime updateTime;

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

    public List<CommentResponse> getReplies() {
        if (replies == null || replies.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(replies));
    }

    public void setReplies(List<CommentResponse> replies) {
        if (replies == null || replies.isEmpty()) {
            this.replies = new ArrayList<>();
            return;
        }
        this.replies = new ArrayList<>(replies);
    }
}
