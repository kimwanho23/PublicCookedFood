package kwh.PublicCookedFood.user.aop.action;

import kwh.PublicCookedFood.user.audit.UserActionAuditPayload;

public class UserActionAuditArgs {

    private final UserActionAuditPayload payload;

    public UserActionAuditArgs(UserActionAuditPayload payload) {
        this.payload = payload == null ? UserActionAuditPayload.empty() : payload;
    }

    public UserActionAuditArgs(Object[] values) {
        this(UserActionAuditPayload.of(values));
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

    public Boolean asBoolean(int index) {
        Object value = valueAt(index);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
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

    public String safeId(Long value) {
        return value == null ? "-" : value.toString();
    }

    public String safeNumber(Integer value) {
        return value == null ? "-" : value.toString();
    }

    public String safeBoolean(Boolean value) {
        return value == null ? "-" : value.toString();
    }

    public String safeText(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }
}
