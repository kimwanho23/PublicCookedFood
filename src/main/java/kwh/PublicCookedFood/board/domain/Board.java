package kwh.PublicCookedFood.board.domain;

import jakarta.persistence.*;
import kwh.PublicCookedFood.common.BaseEntity;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.common.persistence.SoftDeleteStateConverter;
import kwh.PublicCookedFood.account.domain.Account;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.DynamicUpdate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor
@DynamicUpdate
@Table(name = "board", indexes = {
        @Index(name = "idx_board_state_reg_time", columnList = "state, regTime"),
        @Index(name = "idx_board_state_title", columnList = "state, title"),
        @Index(name = "idx_board_account_id", columnList = "account_id"),
        @Index(name = "idx_board_state_section_reg_time", columnList = "state, section_id, regTime"),
        @Index(name = "idx_board_state_hidden_reg_time", columnList = "state, is_hidden_by_report, regTime"),
        @Index(name = "idx_board_state_hidden_section_reg_time", columnList = "state, is_hidden_by_report, section_id, regTime")
})
public class Board extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; //ID

    private String title; //제목

    @Column(columnDefinition = "LONGTEXT")
    private String contents; // 내용

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "id", nullable = false)
    private Account account; //작성자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", referencedColumnName = "id")
    private BoardSection section; // 게시판 탭

    @Version
    private Long version;

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
    public Board(Long id, String title, String contents, Account account, BoardSection section,
                 Long version, SoftDeleteState state,
                 boolean hiddenByReport,
                 List<Likes> likes, List<Comments> comments) {
        this.id = id;
        this.title = title;
        this.contents = contents;
        this.account = account;
        this.section = section;
        this.version = version;
        this.state = state == null ? SoftDeleteState.ACTIVE : state;
        this.hiddenByReport = hiddenByReport;
        this.likes = likes == null ? new ArrayList<>() : new ArrayList<>(likes);
        this.comments = comments == null ? new ArrayList<>() : new ArrayList<>(comments);
    }

    public List<Likes> getLikes() {
        return Collections.unmodifiableList(likes);
    }

    public List<Comments> getComments() {
        return Collections.unmodifiableList(comments);
    }

    public static Board create(String title,
                               String contents,
                               Account account,
                               BoardSection section) {
        return Board.builder()
                .title(requireTitle(title))
                .contents(requireContents(contents))
                .account(requireAccount(account))
                .section(requireSection(section))
                .build();
    }

    public void edit(String title, String contents, BoardSection section) {
        this.title = requireTitle(title);
        this.contents = requireContents(contents);
        this.section = requireSection(section);
    }

    public void updateHiddenByReport(boolean hiddenByReport) {
        this.hiddenByReport = hiddenByReport;
    }

    private static String requireTitle(String title) {
        return requireText(title, "게시글 제목이 비어 있습니다.");
    }

    private static String requireContents(String contents) {
        return requireText(contents, "게시글 내용이 비어 있습니다.");
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static Account requireAccount(Account account) {
        return Objects.requireNonNull(account, "게시글 작성자가 비어 있습니다.");
    }

    private static BoardSection requireSection(BoardSection section) {
        return Objects.requireNonNull(section, "게시판 탭이 비어 있습니다.");
    }
}
