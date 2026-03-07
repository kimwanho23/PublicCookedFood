package kwh.PublicCookedFood.board.domain;

import jakarta.persistence.*;
import kwh.PublicCookedFood.common.BaseEntity;
import kwh.PublicCookedFood.account.domain.Account;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Getter
@Table(name = "comments", indexes = {
        @Index(name = "idx_comments_account_id", columnList = "account_id"),
        @Index(name = "idx_comments_post_state", columnList = "post_id, state"),
        @Index(name = "idx_comments_post_reg_time", columnList = "post_id, regTime"),
        @Index(name = "idx_comments_parent_id", columnList = "parent_id"),
        @Index(name = "idx_comments_post_parent_reg_time", columnList = "post_id, parent_id, regTime"),
        @Index(name = "idx_comments_post_root_reg_time", columnList = "post_id, root_parent_id, regTime"),
        @Index(name = "idx_comments_post_root_depth_reg_time", columnList = "post_id, root_parent_id, depth, regTime"),
        @Index(name = "idx_comments_post_root_comment_path", columnList = "post_id, root_parent_id, comment_path")
})
public class Comments extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 댓글 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Account account; // 작성자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Board board; // 게시글 번호

    @Column(nullable = false)
    private String contents; // 내용

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comments parent; // 답글 인덱스 (첫 댓글은 null)

    @Column(name = "root_parent_id")
    private Long rootParentId; // 댓글 스레드 루트 댓글 ID

    @Column(nullable = false)
    private int depth = 0; // 스레드 깊이

    @Column(name = "comment_path", length = 2048)
    private String commentPath; // 댓글 트리 정렬 경로

    @Column(nullable = false, length = 1)
    @Convert(converter = SoftDeleteStateConverter.class)
    private SoftDeleteState state = SoftDeleteState.ACTIVE; // 댓글 상태 (기본 값 설정)

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comments> replies = new ArrayList<>(); // 답글 리스트

    public Comments() {
    }

    @Builder
    public Comments(Long id,
                    Account account,
                    Board board,
                    String contents,
                    Comments parent,
                    Long rootParentId,
                    Integer depth,
                    String commentPath,
                    SoftDeleteState state,
                    List<Comments> replies) {
        this.id = id;
        this.account = account;
        this.board = board;
        this.contents = contents;
        this.parent = parent;
        this.rootParentId = rootParentId;
        this.depth = depth == null ? 0 : Math.max(depth, 0);
        this.commentPath = commentPath;
        this.state = state == null ? SoftDeleteState.ACTIVE : state;
        this.replies = replies == null ? new ArrayList<>() : new ArrayList<>(replies);
    }

    public List<Comments> getReplies() {
        return Collections.unmodifiableList(replies);
    }

    public Long getEffectiveRootParentId() {
        return rootParentId == null ? id : rootParentId;
    }

    public void initializeThreadMetadata(Long rootParentId, Integer depth, String commentPath) {
        this.rootParentId = rootParentId;
        this.depth = depth == null ? 0 : Math.max(depth, 0);
        this.commentPath = commentPath;
    }

}
