package kwh.PublicCookedFood.user.audit;

import java.util.Arrays;

public record UserActionAuditPayload(Object[] values) {

    private static final UserActionAuditPayload EMPTY = new UserActionAuditPayload(new Object[0]);

    public UserActionAuditPayload {
        values = values == null ? new Object[0] : Arrays.copyOf(values, values.length);
    }

    @Override
    public Object[] values() {
        return Arrays.copyOf(values, values.length);
    }

    public static UserActionAuditPayload empty() {
        return EMPTY;
    }

    public static UserActionAuditPayload of(Object... values) {
        if (values == null || values.length == 0) {
            return EMPTY;
        }
        return new UserActionAuditPayload(values);
    }

    public Object valueAt(int index) {
        if (index < 0 || index >= values.length) {
            return null;
        }
        return values[index];
    }
}
