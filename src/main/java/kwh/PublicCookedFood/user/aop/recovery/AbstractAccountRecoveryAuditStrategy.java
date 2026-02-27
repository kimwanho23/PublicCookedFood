package kwh.PublicCookedFood.user.aop.recovery;

import kwh.PublicCookedFood.user.aop.AccountRecoveryAuditType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class AbstractAccountRecoveryAuditStrategy implements AccountRecoveryAuditStrategy {

    private final Map<AccountRecoveryAuditType, Consumer<AccountRecoveryAuditArgs>> handlers;

    protected AbstractAccountRecoveryAuditStrategy(
            Map<AccountRecoveryAuditType, Consumer<AccountRecoveryAuditArgs>> handlers) {
        EnumMap<AccountRecoveryAuditType, Consumer<AccountRecoveryAuditArgs>> dispatchers =
                new EnumMap<>(AccountRecoveryAuditType.class);
        dispatchers.putAll(Objects.requireNonNull(handlers, "handlers must not be null"));
        if (dispatchers.isEmpty()) {
            throw new IllegalArgumentException("handlers must not be empty");
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
