package kwh.PublicCookedFood.account.audit;

import kwh.PublicCookedFood.account.aop.action.AccountActionAuditArgs;
import kwh.PublicCookedFood.account.aop.action.AccountActionAuditStrategyRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountActionAuditEventListener {

    private final AccountActionAuditStrategyRegistry strategyRegistry;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(AccountActionAuditRequestedEvent event) {
        if (event == null || event.auditType() == null) {
            return;
        }
        try {
            strategyRegistry.handle(event.auditType(), new AccountActionAuditArgs(event.payload()));
        } catch (RuntimeException e) {
            log.error("Failed to process account action audit. type={}", event.auditType(), e);
        }
    }
}
