package kwh.PublicCookedFood.userrecipe.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.domain.SoftDeleteStateConverter;
import kwh.PublicCookedFood.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_recipe_comment", indexes = {
        @Index(name = "idx_user_recipe_comment_account_id", columnList = "account_id"),
        @Index(name = "idx_user_recipe_comment_recipe_state", columnList = "recipe_id, state"),
        @Index(name = "idx_user_recipe_comment_recipe_regtime", columnList = "recipe_id, regTime"),
        @Index(name = "idx_user_recipe_comment_parent_id", columnList = "parent_id"),
        @Index(name = "idx_user_recipe_comment_recipe_parent_regtime", columnList = "recipe_id, parent_id, regTime"),
        @Index(name = "idx_user_recipe_comment_recipe_root_regtime", columnList = "recipe_id, root_parent_id, regTime"),
        @Index(name = "idx_user_recipe_comment_recipe_root_depth_regtime", columnList = "recipe_id, root_parent_id, depth, regTime"),
        @Index(name = "idx_user_recipe_comment_recipe_root_comment_path", columnList = "recipe_id, root_parent_id, comment_path")
})
public class UserRecipeComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserRecipe recipe;

    @Column(nullable = false, length = 1000)
    private String contents;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private UserRecipeComment parent;

    @Column(name = "root_parent_id")
    private Long rootParentId;

    @Column(nullable = false)
    private int depth = 0;

    @Column(name = "comment_path", length = 2048)
    private String commentPath;

    @Column(nullable = false, length = 1)
    @Convert(converter = SoftDeleteStateConverter.class)
    private SoftDeleteState state = SoftDeleteState.ACTIVE;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserRecipeComment> replies = new ArrayList<>();

    @Builder(builderMethodName = "builder")
    private static UserRecipeComment create(Long id,
                                            Account account,
                                            UserRecipe recipe,
                                            String contents,
                                            UserRecipeComment parent,
                                            Long rootParentId,
                                            Integer depth,
                                            String commentPath,
                                            SoftDeleteState state,
                                            List<UserRecipeComment> replies) {
        UserRecipeComment comment = new UserRecipeComment();
        comment.id = id;
        comment.account = account;
        comment.recipe = recipe;
        comment.contents = normalizeRequiredText(contents, 1000, "댓글 내용");
        comment.parent = parent;
        comment.rootParentId = rootParentId;
        comment.depth = depth == null ? 0 : Math.max(depth, 0);
        comment.commentPath = commentPath;
        comment.state = state == null ? SoftDeleteState.ACTIVE : state;
        comment.replies = replies == null ? new ArrayList<>() : new ArrayList<>(replies);
        return comment;
    }

    public List<UserRecipeComment> getReplies() {
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

    public void markDeleted() {
        this.state = SoftDeleteState.DELETED;
    }

    private static String normalizeRequiredText(String rawValue, int maxLength, String fieldLabel) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException(fieldLabel + "은 필수입니다.");
        }
        String trimmed = rawValue.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldLabel + " 길이가 너무 깁니다.");
        }
        return trimmed;
    }
}
