package kwh.PublicCookedFood.account.aop.recovery;

import kwh.PublicCookedFood.account.aop.AccountRecoveryAuditType;

public interface AccountRecoveryAuditStrategy {

    boolean supports(AccountRecoveryAuditType type);

    void handle(AccountRecoveryAuditType type, AccountRecoveryAuditArgs args);
}
