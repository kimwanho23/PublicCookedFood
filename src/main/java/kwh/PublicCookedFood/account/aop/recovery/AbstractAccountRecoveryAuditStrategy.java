package kwh.PublicCookedFood.account.aop.recovery;

import kwh.PublicCookedFood.account.aop.AccountRecoveryAuditType;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class AbstractAccountRecoveryAuditStrategy implements AccountRecoveryAuditStrategy {

    private final Map<AccountRecoveryAuditType, Consumer<AccountRecoveryAuditArgs>> handlers;

    protected AbstractAccountRecoveryAuditStrategy(
            Map<AccountRecoveryAuditType, Consumer<AccountRecoveryAuditArgs>> handlers) {
        EnumMap<AccountRecoveryAuditType, Consumer<AccountRecoveryAuditArgs>> dispatchers =
                new EnumMap<>(AccountRecoveryAuditType.class);
        if (handlers != null) {
            dispatchers.putAll(handlers);
        }
        this.handlers = Map.copyOf(dispatchers);
    }

    protected static EnumMap<AccountRecoveryAuditType, Consumer<AccountRecoveryAuditArgs>> newHandlers() {
        return new EnumMap<>(AccountRecoveryAuditType.class);
    }

    @Override
    public final boolean supports(AccountRecoveryAuditType type) {
        return handlers.containsKey(type);
    }

    @Override
    public final void handle(AccountRecoveryAuditType type, AccountRecoveryAuditArgs args) {
        Consumer<AccountRecoveryAuditArgs> handler = handlers.get(type);
        if (handler == null) {
            throw new IllegalArgumentException("Unsupported recovery audit type: " + type);
        }
        handler.accept(args);
    }
}
