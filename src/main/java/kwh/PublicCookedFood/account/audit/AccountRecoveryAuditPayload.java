package kwh.PublicCookedFood.account.audit;

import java.util.Arrays;

public record AccountRecoveryAuditPayload(Object[] values) {

    private static final AccountRecoveryAuditPayload EMPTY = new AccountRecoveryAuditPayload(new Object[0]);

    public AccountRecoveryAuditPayload {
        values = values == null ? new Object[0] : Arrays.copyOf(values, values.length);
    }

    @Override
    public Object[] values() {
        return Arrays.copyOf(values, values.length);
    }

    public static AccountRecoveryAuditPayload empty() {
        return EMPTY;
    }

    public static AccountRecoveryAuditPayload of(Object... values) {
        if (values == null || values.length == 0) {
            return EMPTY;
        }
        return new AccountRecoveryAuditPayload(values);
    }

    public Object valueAt(int index) {
        if (index < 0 || index >= values.length) {
            return null;
        }
        return values[index];
    }
}
