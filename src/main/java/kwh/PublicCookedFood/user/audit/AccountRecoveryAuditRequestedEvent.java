package kwh.PublicCookedFood.user.audit;

import kwh.PublicCookedFood.user.aop.AccountRecoveryAuditType;

public record AccountRecoveryAuditRequestedEvent(AccountRecoveryAuditType auditType,
                                                 AccountRecoveryAuditPayload payload) {
}
