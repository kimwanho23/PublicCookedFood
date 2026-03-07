package kwh.PublicCookedFood.account.audit;

import kwh.PublicCookedFood.account.aop.AccountRecoveryAuditType;
import kwh.PublicCookedFood.account.aop.AccountActionAuditType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AuditEventDispatcher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishAccountAction(AccountActionAuditType auditType, Object... values) {
        if (auditType == null) {
            return;
        }
        applicationEventPublisher.publishEvent(
                new AccountActionAuditRequestedEvent(auditType, AccountActionAuditPayload.of(values))
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishAccountRecovery(AccountRecoveryAuditType auditType, Object... values) {
        if (auditType == null) {
            return;
        }
        applicationEventPublisher.publishEvent(
                new AccountRecoveryAuditRequestedEvent(auditType, AccountRecoveryAuditPayload.of(values))
        );
    }
}

