package kwh.PublicCookedFood.board.domain;

import jakarta.persistence.*;
import kwh.PublicCookedFood.account.domain.Account;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "likes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_likes_account_board", columnNames = {"account_id", "post_id"})
        }
)
public class Likes {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; //Id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "id", nullable = false)
    private Account account; //작성자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", referencedColumnName = "id", nullable = false)
    private Board board; //게시글 번호

    @Builder
    public Likes(Long id, Account account, Board board) {
        this.id = id;
        this.account = account;
        this.board = board;
    }
}
