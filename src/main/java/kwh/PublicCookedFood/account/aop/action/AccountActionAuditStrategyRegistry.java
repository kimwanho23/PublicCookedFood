package kwh.PublicCookedFood.account.aop.action;

import jakarta.annotation.PostConstruct;
import kwh.PublicCookedFood.account.aop.AccountActionAuditType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class AccountActionAuditStrategyRegistry {

    private final Map<AccountActionAuditType, AccountActionAuditStrategy> strategyMap =
            new EnumMap<>(AccountActionAuditType.class);
    private final List<AccountActionAuditStrategy> strategies;

    public AccountActionAuditStrategyRegistry(List<AccountActionAuditStrategy> strategies) {
        this.strategies = strategies == null ? List.of() : List.copyOf(strategies);
    }

    @PostConstruct
    void initialize() {
        EnumSet<AccountActionAuditType> missingTypes = EnumSet.noneOf(AccountActionAuditType.class);
        for (AccountActionAuditType type : AccountActionAuditType.values()) {
            AccountActionAuditStrategy matched = null;
            for (AccountActionAuditStrategy strategy : strategies) {
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
            log.warn("No account action audit strategies are registered.");
        }
    }

    public void handle(AccountActionAuditType type, AccountActionAuditArgs args) {
        AccountActionAuditStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            return;
        }
        strategy.handle(type, args);
    }
}
