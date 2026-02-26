package kwh.PublicCookedFood.board.domain;

import jakarta.persistence.*;
import kwh.PublicCookedFood.common.BaseEntity;
import kwh.PublicCookedFood.user.domain.Users;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "board", indexes = {
        @Index(name = "idx_board_state_reg_time", columnList = "state, regTime"),
        @Index(name = "idx_board_state_title", columnList = "state, title"),
        @Index(name = "idx_board_user_id", columnList = "user_id"),
        @Index(name = "idx_board_state_section_reg_time", columnList = "state, section_id, regTime"),
        @Index(name = "idx_board_state_like_count", columnList = "state, likeCount")
})
public class Board extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; //ID

    private String title; //제목

    @Column(columnDefinition = "LONGTEXT")
    private String contents; // 내용

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private Users user; //작성자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", referencedColumnName = "id")
    private BoardSection section; // 게시판 탭

    private Long views; // 조회수

    private Long likeCount; // 좋아요 수

    private Long commentCount; //댓글 수

    @Column(nullable = false, length = 1)
    @Convert(converter = SoftDeleteStateConverter.class)
    private SoftDeleteState state = SoftDeleteState.ACTIVE;

    @Column(name = "is_hidden_by_report", nullable = false)
    private boolean hiddenByReport = false;


    @BatchSize(size = 100)
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "board")
    private List<Likes> likes = new ArrayList<>(); // 좋아요

    @BatchSize(size = 100)
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "board")
    private List<Comments> comments = new ArrayList<>(); // 댓글

    @Builder
    public Board(Long id, String title, String contents, Users user, BoardSection section,
                 Long views, Long likeCount, Long commentCount, SoftDeleteState state,
                 boolean hiddenByReport,
                 List<Likes> likes, List<Comments> comments) {
        this.id = id;
        this.title = title;
        this.contents = contents;
        this.user = user;
        this.section = section;
        this.views = views;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.state = state == null ? SoftDeleteState.ACTIVE : state;
        this.hiddenByReport = hiddenByReport;
        this.likes = likes;
        this.comments = comments;
    }

    public void updateHiddenByReport(boolean hiddenByReport) {
        this.hiddenByReport = hiddenByReport;
    }
}
