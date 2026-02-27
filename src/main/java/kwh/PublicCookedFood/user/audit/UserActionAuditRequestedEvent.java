package kwh.PublicCookedFood.user.audit;

import kwh.PublicCookedFood.user.aop.UserActionAuditType;

public record UserActionAuditRequestedEvent(UserActionAuditType auditType,
                                            UserActionAuditPayload payload) {
}
