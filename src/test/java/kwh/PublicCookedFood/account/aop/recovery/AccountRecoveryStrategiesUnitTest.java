package kwh.PublicCookedFood.account.aop.recovery;

import kwh.PublicCookedFood.account.aop.AccountRecoveryAuditType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AccountRecoveryStrategiesUnitTest {

    @Mock
    private AccountRecoveryAuditRecorder recorder;

    private FindEmailAuditStrategy findEmailAuditStrategy;
    private ResetPasswordAuditStrategy resetPasswordAuditStrategy;
    private ResetPasswordCodeAuditStrategy resetPasswordCodeAuditStrategy;

    @BeforeEach
    void setUp() {
        findEmailAuditStrategy = new FindEmailAuditStrategy(recorder);
        resetPasswordAuditStrategy = new ResetPasswordAuditStrategy(recorder);
        resetPasswordCodeAuditStrategy = new ResetPasswordCodeAuditStrategy();
    }

    @Test
    void findEmailStrategy_success_recordsActivity() {
        findEmailAuditStrategy.handle(AccountRecoveryAuditType.FIND_EMAIL_SUCCESS,
                new AccountRecoveryAuditArgs(new Object[]{1L}));

        verify(recorder).record(1L, "ACCOUNT_ACCOUNT_FIND_EMAIL", null);
    }

    @Test
    void findEmailStrategy_notFound_doesNotRecordActivity() {
        findEmailAuditStrategy.handle(AccountRecoveryAuditType.FIND_EMAIL_NOT_FOUND,
                new AccountRecoveryAuditArgs(new Object[]{}));

        verifyNoInteractions(recorder);
    }

    @Test
    void resetPasswordStrategy_success_recordsActivity() {
        resetPasswordAuditStrategy.handle(AccountRecoveryAuditType.RESET_PASSWORD_SUCCESS,
                new AccountRecoveryAuditArgs(new Object[]{2L}));

        verify(recorder).record(2L, "ACCOUNT_ACCOUNT_RESET_PASSWORD", null);
    }

    @Test
    void resetPasswordStrategy_notFound_doesNotRecordActivity() {
        resetPasswordAuditStrategy.handle(AccountRecoveryAuditType.RESET_PASSWORD_NOT_FOUND,
                new AccountRecoveryAuditArgs(new Object[]{}));

        verifyNoInteractions(recorder);
    }

    @Test
    void resetPasswordCodeStrategy_handlesAllCasesWithoutException() {
        assertThatCode(() -> resetPasswordCodeAuditStrategy.handle(
                AccountRecoveryAuditType.RESET_PASSWORD_CODE_MAIL_UNAVAILABLE,
                new AccountRecoveryAuditArgs(new Object[]{"mail@test.com", new IllegalStateException("smtp-down")})))
                .doesNotThrowAnyException();

        assertThatCode(() -> resetPasswordCodeAuditStrategy.handle(
                AccountRecoveryAuditType.RESET_PASSWORD_CODE_ISSUED,
                new AccountRecoveryAuditArgs(new Object[]{"mail@test.com"})))
                .doesNotThrowAnyException();

        assertThatCode(() -> resetPasswordCodeAuditStrategy.handle(
                AccountRecoveryAuditType.RESET_PASSWORD_CODE_NOT_FOUND_OR_SKIPPED,
                new AccountRecoveryAuditArgs(new Object[]{"mail@test.com"})))
                .doesNotThrowAnyException();
    }
}
