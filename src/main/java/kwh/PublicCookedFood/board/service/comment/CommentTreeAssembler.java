package kwh.PublicCookedFood.board.service.comment;

import kwh.PublicCookedFood.board.application.query.view.CommentNodeView;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.dto.response.CommentAuthorView;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class CommentTreeAssembler {

    public CommentNodeView toView(Comments comment) {
        Objects.requireNonNull(comment, "comment");
        Long boardId = comment.getBoard() == null ? null : comment.getBoard().getId();
        return toView(comment, Collections.<Long, List<Comments>>emptyMap(), boardId);
    }

    public CommentNodeView toView(Comments comment,
                                  Map<Long, List<Comments>> repliesByParentId,
                                  Long boardId) {
        Objects.requireNonNull(comment, "comment");
        Objects.requireNonNull(repliesByParentId, "repliesByParentId");
        List<CommentNodeView> replies = repliesByParentId.getOrDefault(comment.getId(), Collections.<Comments>emptyList())
                .stream()
                .map(reply -> toView(reply, repliesByParentId, boardId))
                .collect(Collectors.toList());
        return new CommentNodeView(
                comment.getId(),
                CommentAuthorView.from(comment.getAccount()),
                boardId,
                comment.getContents(),
                comment.getParent() != null ? comment.getParent().getId() : null,
                comment.getState(),
                replies,
                comment.getRegTime(),
                comment.getUpdateTime()
        );
    }
}
