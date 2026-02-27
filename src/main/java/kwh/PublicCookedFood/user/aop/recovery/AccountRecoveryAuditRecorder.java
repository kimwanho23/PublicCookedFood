package kwh.PublicCookedFood.user.aop.recovery;

import kwh.PublicCookedFood.user.service.UserActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountRecoveryAuditRecorder {

    private final UserActivityLogService userActivityLogService;

    public void record(Long userId, String action, String detail) {
        if (userId == null) {
            return;
        }
        userActivityLogService.record(userId, action, detail);
    }
}
