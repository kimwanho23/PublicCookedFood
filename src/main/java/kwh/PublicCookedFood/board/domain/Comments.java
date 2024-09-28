package kwh.PublicCookedFood.board.domain;

import jakarta.persistence.*;
import kwh.PublicCookedFood.common.BaseEntity;
import kwh.PublicCookedFood.user.domain.Users;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
public class Comments extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 댓글 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Users user; // 작성자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Board board; // 게시글 번호

    @Column(nullable = false)
    private String contents; // 내용

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comments parent; // 답글 인덱스 (첫 댓글은 null)

    @Column(nullable = false)
    private String state = "1"; // 댓글 상태 (기본 값 설정)

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comments> replies = new ArrayList<>(); // 답글 리스트


    public Comments() {

    }
    @Builder
    public Comments(Long id, Users user, Board board, String contents, Comments parent, String state, List<Comments> replies) {
        this.id = id;
        this.user = user;
        this.board = board;
        this.contents = contents;
        this.parent = parent;
        this.state = state != null ? state : "1";
        this.replies = replies;
    }


}