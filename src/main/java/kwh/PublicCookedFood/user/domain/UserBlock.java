package kwh.PublicCookedFood.user.domain;

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
@Table(name = "user_block", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_block_blocker_blocked", columnNames = {"blocker_id", "blocked_id"})
}, indexes = {
        @Index(name = "idx_user_block_blocker_regtime", columnList = "blocker_id, regTime"),
        @Index(name = "idx_user_block_blocked_regtime", columnList = "blocked_id, regTime")
})
public class UserBlock extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Users blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Users blocked;

    @Builder
    public UserBlock(Long id, Users blocker, Users blocked) {
        this.id = id;
        this.blocker = blocker;
        this.blocked = blocked;
    }

    public static UserBlock of(Users blocker, Users blocked) {
        return UserBlock.builder()
                .blocker(blocker)
                .blocked(blocked)
                .build();
    }
}
