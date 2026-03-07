package kwh.PublicCookedFood.account.aop.action;

import kwh.PublicCookedFood.account.aop.AccountActionAuditType;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class AbstractAccountActionAuditStrategy implements AccountActionAuditStrategy {

    private final Map<AccountActionAuditType, Consumer<AccountActionAuditArgs>> handlers;

    protected AbstractAccountActionAuditStrategy(Map<AccountActionAuditType, Consumer<AccountActionAuditArgs>> handlers) {
        EnumMap<AccountActionAuditType, Consumer<AccountActionAuditArgs>> dispatchers =
                new EnumMap<>(AccountActionAuditType.class);
        if (handlers != null) {
            dispatchers.putAll(handlers);
        }
        this.handlers = Map.copyOf(dispatchers);
    }

    protected static EnumMap<AccountActionAuditType, Consumer<AccountActionAuditArgs>> newHandlers() {
        return new EnumMap<>(AccountActionAuditType.class);
    }

    @Override
    public final boolean supports(AccountActionAuditType type) {
        return handlers.containsKey(type);
    }

    @Override
    public final void handle(AccountActionAuditType type, AccountActionAuditArgs args) {
        Consumer<AccountActionAuditArgs> handler = handlers.get(type);
        if (handler == null) {
            throw new IllegalArgumentException("Unsupported audit type: " + type);
        }
        handler.accept(args);
    }
}
