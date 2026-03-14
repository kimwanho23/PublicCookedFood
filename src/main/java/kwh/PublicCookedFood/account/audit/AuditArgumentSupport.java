package kwh.PublicCookedFood.account.audit;

import java.util.Objects;

public final class AuditArgumentSupport {

    private static final String MISSING = "-";

    private AuditArgumentSupport() {
    }

    public static Long asLong(Object value) {
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

    public static Integer asInteger(Object value) {
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

    public static Boolean asBoolean(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return null;
    }

    public static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    public static Throwable asThrowable(Object value) {
        if (value instanceof Throwable throwable) {
            return throwable;
        }
        return null;
    }

    public static <T> T asType(Object value, Class<T> type) {
        Objects.requireNonNull(type, "type");
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }

    public static String displayId(Long value) {
        return value == null ? MISSING : String.valueOf(value);
    }

    public static String displayNumber(Integer value) {
        return value == null ? MISSING : String.valueOf(value);
    }

    public static String displayBoolean(Boolean value) {
        return value == null ? MISSING : String.valueOf(value);
    }

    public static String displayText(String value) {
        if (value == null || value.isBlank()) {
            return MISSING;
        }
        return value;
    }
}
