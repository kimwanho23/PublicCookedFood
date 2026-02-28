package kwh.PublicCookedFood.user.aop.action;

import jakarta.annotation.PostConstruct;
import kwh.PublicCookedFood.user.aop.UserActionAuditType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class UserActionAuditStrategyRegistry {

    private final Map<UserActionAuditType, UserActionAuditStrategy> strategyMap =
            new EnumMap<>(UserActionAuditType.class);
    private final List<UserActionAuditStrategy> strategies;

    public UserActionAuditStrategyRegistry(List<UserActionAuditStrategy> strategies) {
        this.strategies = strategies == null ? List.of() : List.copyOf(strategies);
    }

    @PostConstruct
    void initialize() {
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
        if (strategyMap.isEmpty()) {
            log.warn("No user action audit strategies are registered.");
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
