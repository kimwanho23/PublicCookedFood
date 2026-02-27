package kwh.PublicCookedFood.user.aop.recovery;

import kwh.PublicCookedFood.user.aop.AccountRecoveryAuditType;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class AccountRecoveryAuditStrategyRegistry {

    private final Map<AccountRecoveryAuditType, AccountRecoveryAuditStrategy> strategyMap =
            new EnumMap<>(AccountRecoveryAuditType.class);

    public AccountRecoveryAuditStrategyRegistry(List<AccountRecoveryAuditStrategy> strategies) {
        EnumSet<AccountRecoveryAuditType> missingTypes = EnumSet.noneOf(AccountRecoveryAuditType.class);
        for (AccountRecoveryAuditType type : AccountRecoveryAuditType.values()) {
            AccountRecoveryAuditStrategy matched = null;
            for (AccountRecoveryAuditStrategy strategy : strategies) {
                if (!strategy.supports(type)) {
                    continue;
                }
                if (matched != null) {
                    throw new IllegalStateException("Duplicate recovery audit strategy for type: " + type);
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
            throw new IllegalStateException("Missing recovery audit strategy for types: " + missingTypes);
        }
    }

    public void handle(AccountRecoveryAuditType type, AccountRecoveryAuditArgs args) {
        AccountRecoveryAuditStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            return;
        }
        strategy.handle(type, args);
    }
}
