package kwh.PublicCookedFood.account.audit;

import kwh.PublicCookedFood.account.aop.AccountActionAuditType;

public record AccountActionAuditRequestedEvent(AccountActionAuditType auditType,
                                            AccountActionAuditPayload payload) {
}
