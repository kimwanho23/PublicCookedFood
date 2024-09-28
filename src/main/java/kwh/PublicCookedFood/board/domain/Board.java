package kwh.PublicCookedFood.board.domain;

import jakarta.persistence.*;
import kwh.PublicCookedFood.board.dto.BoardDto;
import kwh.PublicCookedFood.common.BaseEntity;
import kwh.PublicCookedFood.user.domain.Users;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "board")
public class Board extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; //ID

    private String title; //제목

    @Column(columnDefinition = "LONGTEXT")
    private String contents; // 내용

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private Users user; //작성자

    private Long views; // 조회수

    private Long likeCount; // 좋아요

    private Long commentCount; //댓글 수

    private String state;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "board")
    private List<Likes> likes = new ArrayList<>(); // 좋아요

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "board")
    private List<Comments> comments = new ArrayList<>(); // 댓글

/*    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "board")
    private List<Files> files = new ArrayList<>(); // 파일

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "board")
    private List<Images> images = new ArrayList<>(); // 이미지*/

    @Builder
    public Board(Long id, String title, String contents, Users user, Long views, Long likeCount, Long commentCount,
                 String state, List<Likes> likes, List<Comments> comments) {
        this.id = id;
        this.title = title;
        this.contents = contents;
        this.user = user;
        this.views = views;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.state = state;
        this.likes = likes;
        this.comments = comments;
    }

    public BoardDto toResponseDto(){
        return BoardDto.builder()
                .id(id)
                .title(title)
                .contents(contents)
                .userId(user)
                .views(views)
                .likesCount(likeCount)
                .commentsCount(commentCount)
                .state(state)
                .likes(likes)
                .comments(comments)
                .build();
    }


}
