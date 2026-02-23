package kwh.PublicCookedFood.board.dto;

import kwh.PublicCookedFood.common.BaseEntity;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Getter
@Setter
@NoArgsConstructor
@ToString
public class CommentsDto extends BaseEntity {

    private Long id;   // 댓글 ID

    private Long userId; // 작성자 ID

    private String name;

    private Long boardId; // 게시글 번호 ID

    private String contents; // 내용

    private Long parentId; // 답글 인덱스 (첫 댓글은 null)

    private String state;

    private List<CommentsDto> replies = new ArrayList<>(); // 답글 리스트

    private boolean areAllRepliesDeleted;

    public boolean areAllRepliesDeleted() {
        return Objects.equals(this.getState(), "0") && replies.stream().allMatch(reply -> Objects.equals(reply.getState(), "0"));
    }

}