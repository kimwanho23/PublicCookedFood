package kwh.PublicCookedFood.account.aop.recovery;

import kwh.PublicCookedFood.account.audit.AccountRecoveryAuditPayload;
import kwh.PublicCookedFood.account.audit.AuditArgumentSupport;

import java.util.Objects;

public class AccountRecoveryAuditArgs {

    private final AccountRecoveryAuditPayload payload;

    public AccountRecoveryAuditArgs(AccountRecoveryAuditPayload payload) {
        this.payload = Objects.requireNonNullElse(payload, AccountRecoveryAuditPayload.empty());
    }

    public AccountRecoveryAuditArgs(Object[] values) {
        this(AccountRecoveryAuditPayload.of(values));
    }

    private Object valueAt(int index) {
        return payload.valueAt(index);
    }

    public Long asLong(int index) {
        return AuditArgumentSupport.asLong(valueAt(index));
    }

    public String asString(int index) {
        return AuditArgumentSupport.asString(valueAt(index));
    }

    public Throwable asThrowable(int index) {
        return AuditArgumentSupport.asThrowable(valueAt(index));
    }

    public String displayId(Long value) {
        return AuditArgumentSupport.displayId(value);
    }

    public String displayText(String value) {
        return AuditArgumentSupport.displayText(value);
    }
}
