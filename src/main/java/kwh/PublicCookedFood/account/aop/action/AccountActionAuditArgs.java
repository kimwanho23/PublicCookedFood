package kwh.PublicCookedFood.account.aop.action;

import kwh.PublicCookedFood.account.audit.AccountActionAuditPayload;
import kwh.PublicCookedFood.account.audit.AuditArgumentSupport;

import java.util.Objects;
import java.util.Optional;

public class AccountActionAuditArgs {

    private final AccountActionAuditPayload payload;

    public AccountActionAuditArgs(AccountActionAuditPayload payload) {
        this.payload = Objects.requireNonNullElse(payload, AccountActionAuditPayload.empty());
    }

    public AccountActionAuditArgs(Object[] values) {
        this(AccountActionAuditPayload.of(values));
    }

    private Object valueAt(int index) {
        return payload.valueAt(index);
    }

    public Long asLong(int index) {
        return AuditArgumentSupport.asLong(valueAt(index));
    }

    public Integer asInteger(int index) {
        return AuditArgumentSupport.asInteger(valueAt(index));
    }

    public Optional<Boolean> asBoolean(int index) {
        return AuditArgumentSupport.asBoolean(valueAt(index));
    }

    public String asString(int index) {
        return AuditArgumentSupport.asString(valueAt(index));
    }

    public Throwable asThrowable(int index) {
        return AuditArgumentSupport.asThrowable(valueAt(index));
    }

    public <T> T asType(int index, Class<T> type) {
        return AuditArgumentSupport.asType(valueAt(index), type);
    }

    public String displayId(Long value) {
        return AuditArgumentSupport.displayId(value);
    }

    public String displayNumber(Integer value) {
        return AuditArgumentSupport.displayNumber(value);
    }

    public String displayBoolean(Boolean value) {
        return AuditArgumentSupport.displayBoolean(value);
    }

    public String displayBooleanAt(int index) {
        return AuditArgumentSupport.displayBoolean(asBoolean(index).orElse(null));
    }

    public String displayText(String value) {
        return AuditArgumentSupport.displayText(value);
    }
}
