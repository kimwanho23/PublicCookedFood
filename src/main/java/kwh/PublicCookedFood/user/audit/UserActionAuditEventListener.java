package kwh.PublicCookedFood.user.audit;

import kwh.PublicCookedFood.user.aop.action.UserActionAuditArgs;
import kwh.PublicCookedFood.user.aop.action.UserActionAuditStrategyRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserActionAuditEventListener {

    private final UserActionAuditStrategyRegistry strategyRegistry;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserActionAuditRequestedEvent event) {
        if (event == null || event.auditType() == null) {
            return;
        }
        try {
            strategyRegistry.handle(event.auditType(), new UserActionAuditArgs(event.payload()));
        } catch (RuntimeException e) {
            log.error("Failed to process user action audit. type={}", event.auditType(), e);
        }
    }
}
