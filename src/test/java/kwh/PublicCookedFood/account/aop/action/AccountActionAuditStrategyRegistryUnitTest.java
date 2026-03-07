package kwh.PublicCookedFood.account.aop.action;

import kwh.PublicCookedFood.account.aop.AccountActionAuditType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountActionAuditStrategyRegistryUnitTest {

    @Test
    void constructor_throwsWhenDuplicateStrategyIsRegistered() {
        AccountActionAuditStrategyRegistry registry = new AccountActionAuditStrategyRegistry(List.of(
                new FixedStrategy(AccountActionAuditType.ACCOUNT_SIGNUP),
                new FixedStrategy(AccountActionAuditType.ACCOUNT_SIGNUP)));

        assertThatThrownBy(registry::initialize)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate audit strategy");
    }

    @Test
    void handle_delegatesToMatchedStrategy() {
        RecordingStrategy strategy = new RecordingStrategy(AccountActionAuditType.ACCOUNT_SIGNUP);
        FixedStrategy fallbackStrategy = new FixedStrategy(EnumSet.complementOf(EnumSet.of(AccountActionAuditType.ACCOUNT_SIGNUP)));
        AccountActionAuditStrategyRegistry registry = new AccountActionAuditStrategyRegistry(List.of(strategy, fallbackStrategy));
        AccountActionAuditArgs args = new AccountActionAuditArgs(new Object[]{1L, "test@example.com"});
        registry.initialize();

        registry.handle(AccountActionAuditType.ACCOUNT_SIGNUP, args);

        assertThat(strategy.handledCount).isEqualTo(1);
        assertThat(strategy.lastType).isEqualTo(AccountActionAuditType.ACCOUNT_SIGNUP);
        assertThat(strategy.lastArgs).isSameAs(args);
    }

    @Test
    void constructor_throwsWhenStrategyIsMissing() {
        RecordingStrategy strategy = new RecordingStrategy(AccountActionAuditType.ACCOUNT_SIGNUP);
        AccountActionAuditStrategyRegistry registry = new AccountActionAuditStrategyRegistry(List.of(strategy));

        assertThatThrownBy(registry::initialize)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing audit strategy");
    }

    private static final class FixedStrategy implements AccountActionAuditStrategy {
        private final EnumSet<AccountActionAuditType> supportedTypes;

        private FixedStrategy(AccountActionAuditType supportedType) {
            this.supportedTypes = EnumSet.of(supportedType);
        }

        private FixedStrategy(EnumSet<AccountActionAuditType> supportedTypes) {
            this.supportedTypes = EnumSet.copyOf(supportedTypes);
        }

        @Override
        public boolean supports(AccountActionAuditType type) {
            return supportedTypes.contains(type);
        }

        @Override
        public void handle(AccountActionAuditType type, AccountActionAuditArgs args) {
            // no-op
        }
    }

    private static final class RecordingStrategy implements AccountActionAuditStrategy {
        private final EnumSet<AccountActionAuditType> supportedTypes;
        private int handledCount;
        private AccountActionAuditType lastType;
        private AccountActionAuditArgs lastArgs;

        private RecordingStrategy(AccountActionAuditType supportedType) {
            this.supportedTypes = EnumSet.of(supportedType);
        }

        @Override
        public boolean supports(AccountActionAuditType type) {
            return supportedTypes.contains(type);
        }

        @Override
        public void handle(AccountActionAuditType type, AccountActionAuditArgs args) {
            handledCount++;
            lastType = type;
            lastArgs = args;
        }
    }
}
