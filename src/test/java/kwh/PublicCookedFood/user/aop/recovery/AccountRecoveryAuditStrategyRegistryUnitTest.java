package kwh.PublicCookedFood.user.aop.recovery;

import kwh.PublicCookedFood.user.aop.AccountRecoveryAuditType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountRecoveryAuditStrategyRegistryUnitTest {

    @Test
    void constructor_throwsWhenDuplicateStrategyIsRegistered() {
        assertThatThrownBy(() -> new AccountRecoveryAuditStrategyRegistry(List.of(
                new FixedStrategy(AccountRecoveryAuditType.FIND_EMAIL_SUCCESS),
                new FixedStrategy(AccountRecoveryAuditType.FIND_EMAIL_SUCCESS))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate recovery audit strategy");
    }

    @Test
    void handle_delegatesToMatchedStrategy() {
        RecordingStrategy strategy = new RecordingStrategy(AccountRecoveryAuditType.RESET_PASSWORD_SUCCESS);
        FixedStrategy fallbackStrategy = new FixedStrategy(
                EnumSet.complementOf(EnumSet.of(AccountRecoveryAuditType.RESET_PASSWORD_SUCCESS)));
        AccountRecoveryAuditStrategyRegistry registry = new AccountRecoveryAuditStrategyRegistry(List.of(strategy, fallbackStrategy));
        AccountRecoveryAuditArgs args = new AccountRecoveryAuditArgs(new Object[]{1L});

        registry.handle(AccountRecoveryAuditType.RESET_PASSWORD_SUCCESS, args);

        assertThat(strategy.handledCount).isEqualTo(1);
        assertThat(strategy.lastType).isEqualTo(AccountRecoveryAuditType.RESET_PASSWORD_SUCCESS);
        assertThat(strategy.lastArgs).isSameAs(args);
    }

    @Test
    void constructor_throwsWhenStrategyIsMissing() {
        RecordingStrategy strategy = new RecordingStrategy(AccountRecoveryAuditType.RESET_PASSWORD_SUCCESS);
        assertThatThrownBy(() -> new AccountRecoveryAuditStrategyRegistry(List.of(strategy)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing recovery audit strategy");
    }

    private static final class FixedStrategy implements AccountRecoveryAuditStrategy {
        private final EnumSet<AccountRecoveryAuditType> supportedTypes;

        private FixedStrategy(AccountRecoveryAuditType supportedType) {
            this.supportedTypes = EnumSet.of(supportedType);
        }

        private FixedStrategy(EnumSet<AccountRecoveryAuditType> supportedTypes) {
            this.supportedTypes = EnumSet.copyOf(supportedTypes);
        }

        @Override
        public boolean supports(AccountRecoveryAuditType type) {
            return supportedTypes.contains(type);
        }

        @Override
        public void handle(AccountRecoveryAuditType type, AccountRecoveryAuditArgs args) {
            // no-op
        }
    }

    private static final class RecordingStrategy implements AccountRecoveryAuditStrategy {
        private final EnumSet<AccountRecoveryAuditType> supportedTypes;
        private int handledCount;
        private AccountRecoveryAuditType lastType;
        private AccountRecoveryAuditArgs lastArgs;

        private RecordingStrategy(AccountRecoveryAuditType supportedType) {
            this.supportedTypes = EnumSet.of(supportedType);
        }

        @Override
        public boolean supports(AccountRecoveryAuditType type) {
            return supportedTypes.contains(type);
        }

        @Override
        public void handle(AccountRecoveryAuditType type, AccountRecoveryAuditArgs args) {
            handledCount++;
            lastType = type;
            lastArgs = args;
        }
    }
}
