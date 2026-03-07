package kwh.PublicCookedFood.account.aop.action;

import kwh.PublicCookedFood.account.aop.AccountActionAuditType;

public interface AccountActionAuditStrategy {

    boolean supports(AccountActionAuditType type);

    void handle(AccountActionAuditType type, AccountActionAuditArgs args);
}
