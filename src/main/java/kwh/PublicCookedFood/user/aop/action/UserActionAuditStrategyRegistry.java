package kwh.PublicCookedFood.user.aop.action;

import kwh.PublicCookedFood.user.aop.UserActionAuditType;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class UserActionAuditStrategyRegistry {

    private final Map<UserActionAuditType, UserActionAuditStrategy> strategyMap =
            new EnumMap<>(UserActionAuditType.class);

    public UserActionAuditStrategyRegistry(List<UserActionAuditStrategy> strategies) {
        EnumSet<UserActionAuditType> missingTypes = EnumSet.noneOf(UserActionAuditType.class);
        for (UserActionAuditType type : UserActionAuditType.values()) {
            UserActionAuditStrategy matched = null;
            for (UserActionAuditStrategy strategy : strategies) {
                if (!strategy.supports(type)) {
                    continue;
                }
                if (matched != null) {
                    throw new IllegalStateException("Duplicate audit strategy for type: " + type);
                }
                matched = strategy;
            }
            if (matched != null) {
                strategyMap.put(type, matched);
            } else {
                missingTypes.add(type);
            }
        }
        if (!missingTypes.isEmpty()) {
            throw new IllegalStateException("Missing audit strategy for types: " + missingTypes);
        }
    }

    public void handle(UserActionAuditType type, UserActionAuditArgs args) {
        UserActionAuditStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            return;
        }
        strategy.handle(type, args);
    }
}
