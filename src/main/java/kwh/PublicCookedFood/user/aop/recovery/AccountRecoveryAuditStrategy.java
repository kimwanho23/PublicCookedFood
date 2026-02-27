package kwh.PublicCookedFood.user.aop.recovery;

import kwh.PublicCookedFood.user.aop.AccountRecoveryAuditType;

public interface AccountRecoveryAuditStrategy {

    boolean supports(AccountRecoveryAuditType type);

    void handle(AccountRecoveryAuditType type, AccountRecoveryAuditArgs args);
}
