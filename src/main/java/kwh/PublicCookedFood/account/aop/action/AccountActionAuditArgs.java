package kwh.PublicCookedFood.account.aop.action;

import kwh.PublicCookedFood.account.audit.AccountActionAuditPayload;

import java.util.Optional;

public class AccountActionAuditArgs {

    private final AccountActionAuditPayload payload;

    public AccountActionAuditArgs(AccountActionAuditPayload payload) {
        this.payload = payload == null ? AccountActionAuditPayload.empty() : payload;
    }

    public AccountActionAuditArgs(Object[] values) {
        this(AccountActionAuditPayload.of(values));
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

    public Integer asInteger(int index) {
        Object value = valueAt(index);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer integerValue) {
            return integerValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    public Optional<Boolean> asBoolean(int index) {
        Object value = valueAt(index);
        if (value instanceof Boolean booleanValue) {
            return Optional.of(booleanValue);
        }
        return Optional.empty();
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

    public String safeId(Long value) {
        return value == null ? "-" : value.toString();
    }

    public String safeNumber(Integer value) {
        return value == null ? "-" : value.toString();
    }

    public String safeBoolean(Optional<Boolean> value) {
        if (value == null || value.isEmpty()) {
            return "-";
        }
        return value.get().toString();
    }

    public String safeText(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }
}
