package kwh.PublicCookedFood.user.domain;

import jakarta.persistence.*;
import kwh.PublicCookedFood.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_activity_log", indexes = {
        @Index(name = "idx_user_activity_user_regtime", columnList = "user_id, regTime"),
        @Index(name = "idx_user_activity_action_regtime", columnList = "action, regTime")
})
public class UserActivityLog extends BaseTimeEntity {

    private static final int MAX_ACTION_LENGTH = 80;
    private static final int MAX_DETAIL_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(nullable = false, length = MAX_ACTION_LENGTH)
    private String action;

    @Column(length = MAX_DETAIL_LENGTH)
    private String detail;

    @Builder
    public UserActivityLog(Long id, Users user, String action, String detail) {
        this.id = id;
        this.user = user;
        this.action = normalizeAction(action);
        this.detail = normalizeDetail(detail);
    }

    private String normalizeAction(String rawAction) {
        if (rawAction == null) {
            return "UNKNOWN";
        }
        String normalized = rawAction.trim();
        if (normalized.isEmpty()) {
            return "UNKNOWN";
        }
        if (normalized.length() <= MAX_ACTION_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_ACTION_LENGTH);
    }

    private String normalizeDetail(String rawDetail) {
        if (rawDetail == null) {
            return null;
        }
        String normalized = rawDetail.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() <= MAX_DETAIL_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_DETAIL_LENGTH);
    }
}
