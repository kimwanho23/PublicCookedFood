package kwh.PublicCookedFood.account.audit;

import kwh.PublicCookedFood.account.aop.AccountRecoveryAuditType;

public record AccountRecoveryAuditRequestedEvent(AccountRecoveryAuditType auditType,
                                                 AccountRecoveryAuditPayload payload) {
}
