package kwh.PublicCookedFood.user.aop.action;

import kwh.PublicCookedFood.user.aop.UserActionAuditType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserActionAuditStrategyRegistryUnitTest {

    @Test
    void constructor_throwsWhenDuplicateStrategyIsRegistered() {
        assertThatThrownBy(() -> new UserActionAuditStrategyRegistry(List.of(
                new FixedStrategy(UserActionAuditType.USER_SIGNUP),
                new FixedStrategy(UserActionAuditType.USER_SIGNUP))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate audit strategy");
    }

    @Test
    void handle_delegatesToMatchedStrategy() {
        RecordingStrategy strategy = new RecordingStrategy(UserActionAuditType.USER_SIGNUP);
        FixedStrategy fallbackStrategy = new FixedStrategy(EnumSet.complementOf(EnumSet.of(UserActionAuditType.USER_SIGNUP)));
        UserActionAuditStrategyRegistry registry = new UserActionAuditStrategyRegistry(List.of(strategy, fallbackStrategy));
        UserActionAuditArgs args = new UserActionAuditArgs(new Object[]{1L, "test@example.com"});

        registry.handle(UserActionAuditType.USER_SIGNUP, args);

        assertThat(strategy.handledCount).isEqualTo(1);
        assertThat(strategy.lastType).isEqualTo(UserActionAuditType.USER_SIGNUP);
        assertThat(strategy.lastArgs).isSameAs(args);
    }

    @Test
    void constructor_throwsWhenStrategyIsMissing() {
        RecordingStrategy strategy = new RecordingStrategy(UserActionAuditType.USER_SIGNUP);
        assertThatThrownBy(() -> new UserActionAuditStrategyRegistry(List.of(strategy)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing audit strategy");
    }

    private static final class FixedStrategy implements UserActionAuditStrategy {
        private final EnumSet<UserActionAuditType> supportedTypes;

        private FixedStrategy(UserActionAuditType supportedType) {
            this.supportedTypes = EnumSet.of(supportedType);
        }

        private FixedStrategy(EnumSet<UserActionAuditType> supportedTypes) {
            this.supportedTypes = EnumSet.copyOf(supportedTypes);
        }

        @Override
        public boolean supports(UserActionAuditType type) {
            return supportedTypes.contains(type);
        }

        @Override
        public void handle(UserActionAuditType type, UserActionAuditArgs args) {
            // no-op
        }
    }

    private static final class RecordingStrategy implements UserActionAuditStrategy {
        private final EnumSet<UserActionAuditType> supportedTypes;
        private int handledCount;
        private UserActionAuditType lastType;
        private UserActionAuditArgs lastArgs;

        private RecordingStrategy(UserActionAuditType supportedType) {
            this.supportedTypes = EnumSet.of(supportedType);
        }

        @Override
        public boolean supports(UserActionAuditType type) {
            return supportedTypes.contains(type);
        }

        @Override
        public void handle(UserActionAuditType type, UserActionAuditArgs args) {
            handledCount++;
            lastType = type;
            lastArgs = args;
        }
    }
}
