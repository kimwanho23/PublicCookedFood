package kwh.PublicCookedFood.user.aop.recovery;

import kwh.PublicCookedFood.user.audit.AccountRecoveryAuditPayload;

public class AccountRecoveryAuditArgs {

    private final AccountRecoveryAuditPayload payload;

    public AccountRecoveryAuditArgs(AccountRecoveryAuditPayload payload) {
        this.payload = payload == null ? AccountRecoveryAuditPayload.empty() : payload;
    }

    public AccountRecoveryAuditArgs(Object[] values) {
        this(AccountRecoveryAuditPayload.of(values));
    }

    private Object valueAt(int index) {
        return payload.valueAt(index);
    }

    public Long asLong(int index) {
        Object value = valueAt(index);
        if (value == null) {
            return null;
        }
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public String asString(int index) {
        Object value = valueAt(index);
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    public Throwable asThrowable(int index) {
        Object value = valueAt(index);
        if (value == null) {
            return null;
        }
        if (value instanceof Throwable throwable) {
            return throwable;
        }
        return null;
    }
}
