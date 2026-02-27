package kwh.PublicCookedFood.user.audit;

import kwh.PublicCookedFood.user.aop.AccountRecoveryAuditType;
import kwh.PublicCookedFood.user.aop.UserActionAuditType;
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
    public void publishUserAction(UserActionAuditType auditType, Object... values) {
        if (auditType == null) {
            return;
        }
        applicationEventPublisher.publishEvent(
                new UserActionAuditRequestedEvent(auditType, UserActionAuditPayload.of(values))
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
