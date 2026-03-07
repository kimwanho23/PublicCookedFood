package kwh.PublicCookedFood.account.audit;

import kwh.PublicCookedFood.account.aop.recovery.AccountRecoveryAuditArgs;
import kwh.PublicCookedFood.account.aop.recovery.AccountRecoveryAuditStrategyRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountRecoveryAuditEventListener {

    private final AccountRecoveryAuditStrategyRegistry strategyRegistry;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(AccountRecoveryAuditRequestedEvent event) {
        if (event == null || event.auditType() == null) {
            return;
        }
        try {
            strategyRegistry.handle(event.auditType(), new AccountRecoveryAuditArgs(event.payload()));
        } catch (RuntimeException e) {
            log.error("Failed to process account recovery audit. type={}", event.auditType(), e);
        }
    }
}
