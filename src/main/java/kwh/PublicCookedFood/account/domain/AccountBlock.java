package kwh.PublicCookedFood.account.domain;

import jakarta.persistence.*;
import kwh.PublicCookedFood.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "account_block", uniqueConstraints = {
        @UniqueConstraint(name = "uk_account_block_blocker_blocked", columnNames = {"blocker_id", "blocked_id"})
}, indexes = {
        @Index(name = "idx_account_block_blocker_regtime", columnList = "blocker_id, regTime"),
        @Index(name = "idx_account_block_blocked_regtime", columnList = "blocked_id, regTime")
})
public class AccountBlock extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Account blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Account blocked;

    @Builder
    public AccountBlock(Long id, Account blocker, Account blocked) {
        this.id = id;
        this.blocker = blocker;
        this.blocked = blocked;
    }

    public static AccountBlock of(Account blocker, Account blocked) {
        return AccountBlock.builder()
                .blocker(blocker)
                .blocked(blocked)
                .build();
    }
}
