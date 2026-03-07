package kwh.PublicCookedFood.account.audit;

import java.util.Arrays;

public record AccountActionAuditPayload(Object[] values) {

    private static final AccountActionAuditPayload EMPTY = new AccountActionAuditPayload(new Object[0]);

    public AccountActionAuditPayload {
        values = values == null ? new Object[0] : Arrays.copyOf(values, values.length);
    }

    @Override
    public Object[] values() {
        return Arrays.copyOf(values, values.length);
    }

    public static AccountActionAuditPayload empty() {
        return EMPTY;
    }

    public static AccountActionAuditPayload of(Object... values) {
        if (values == null || values.length == 0) {
            return EMPTY;
        }
        return new AccountActionAuditPayload(values);
    }

    public Object valueAt(int index) {
        if (index < 0 || index >= values.length) {
            return null;
        }
        return values[index];
    }
}
