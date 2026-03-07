package kwh.PublicCookedFood.account.aop.action;

import kwh.PublicCookedFood.account.service.AccountActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountActionAuditRecorder {

    private final AccountActivityLogService accountActivityLogService;

    public void record(Long accountId, String action, String detail) {
        if (accountId == null) {
            return;
        }
        accountActivityLogService.record(accountId, action, detail);
    }
}

