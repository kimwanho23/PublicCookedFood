package kwh.PublicCookedFood.board.domain;

import jakarta.persistence.*;
import kwh.PublicCookedFood.common.BaseTimeEntity;
import kwh.PublicCookedFood.user.domain.Users;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "board_scrap", uniqueConstraints = {
        @UniqueConstraint(name = "uk_board_scrap_user_board", columnNames = {"user_id", "board_id"})
}, indexes = {
        @Index(name = "idx_board_scrap_user_regtime", columnList = "user_id, regTime"),
        @Index(name = "idx_board_scrap_board_regtime", columnList = "board_id, regTime")
})
public class BoardScrap extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Board board;

    @Builder
    public BoardScrap(Long id, Users user, Board board) {
        this.id = id;
        this.user = user;
        this.board = board;
    }
}
