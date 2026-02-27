package kwh.PublicCookedFood.user.aop.action;

import kwh.PublicCookedFood.user.aop.UserActionAuditType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class AbstractUserActionAuditStrategy implements UserActionAuditStrategy {

    private final Map<UserActionAuditType, Consumer<UserActionAuditArgs>> handlers;

    protected AbstractUserActionAuditStrategy(Map<UserActionAuditType, Consumer<UserActionAuditArgs>> handlers) {
        EnumMap<UserActionAuditType, Consumer<UserActionAuditArgs>> dispatchers =
                new EnumMap<>(UserActionAuditType.class);
        dispatchers.putAll(Objects.requireNonNull(handlers, "handlers must not be null"));
        if (dispatchers.isEmpty()) {
            throw new IllegalArgumentException("handlers must not be empty");
        }
        this.handlers = Map.copyOf(dispatchers);
    }

    protected static EnumMap<UserActionAuditType, Consumer<UserActionAuditArgs>> newHandlers() {
        return new EnumMap<>(UserActionAuditType.class);
    }

    @Override
    public final boolean supports(UserActionAuditType type) {
        return handlers.containsKey(type);
    }

    @Override
    public final void handle(UserActionAuditType type, UserActionAuditArgs args) {
        Consumer<UserActionAuditArgs> handler = handlers.get(type);
        if (handler == null) {
            throw new IllegalArgumentException("Unsupported audit type: " + type);
        }
        handler.accept(args);
    }
}
