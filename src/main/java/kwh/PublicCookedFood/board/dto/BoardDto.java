package kwh.PublicCookedFood.board.dto;

import kwh.PublicCookedFood.board.domain.*;
import kwh.PublicCookedFood.common.BaseEntity;
import kwh.PublicCookedFood.user.domain.Users;
import lombok.*;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@ToString
public class BoardDto extends BaseEntity {

    private Long id; // 게시글 ID

    private String title; //제목

    private String contents; // 내용

    private Users userId; // 작성자

    private Long views; //조회수

    private Long likesCount; // 좋아요

    private Long commentsCount; //댓글 수

    private String state;

    private List<Likes> likes;

    private List<Comments> comments;


    @Builder
    public BoardDto(Long id, String title, String contents, Users userId, Long views, Long likesCount, Long commentsCount, String state,
                    List<Likes> likes, List<Comments> comments) {
        this.id = id;
        this.title = title;
        this.contents = contents;
        this.userId = userId;
        this.views = views;
        this.likesCount = likesCount;
        this.commentsCount = commentsCount;
        this.state = state;
        this.likes = likes;
        this.comments = comments;
    }

    public Board toEntity() {
        return Board.builder()
                .id(id)
                .title(title)
                .contents(contents)
                .user(userId)
                .views(views)
                .likeCount(likesCount)
                .commentCount(commentsCount)
                .state(state)
                .likes(likes)
                .comments(comments)
                .build();
    }
}
