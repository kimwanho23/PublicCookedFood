package kwh.PublicCookedFood.user.aop.action;

import kwh.PublicCookedFood.user.aop.UserActionAuditType;

public interface UserActionAuditStrategy {

    boolean supports(UserActionAuditType type);

    void handle(UserActionAuditType type, UserActionAuditArgs args);
}
