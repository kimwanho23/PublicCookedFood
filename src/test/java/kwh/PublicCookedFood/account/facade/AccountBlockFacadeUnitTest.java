package kwh.PublicCookedFood.account.facade;

import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.account.audit.AccountAuditPublisher;
import kwh.PublicCookedFood.account.error.AccountErrorCode;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountBlockFacadeUnitTest {

    @Mock
    private AccountBlockService accountBlockService;

    @Mock
    private AccountAuditPublisher accountAuditPublisher;

    @InjectMocks
    private AccountBlockFacade accountBlockFacade;

    @Test
    void block_returnsFailureMessageWhenServiceThrowsAppException() {
        when(accountBlockService.block(1L, 1L))
                .thenThrow(new AppException(AccountErrorCode.ACCOUNT_BLOCK_SELF_FORBIDDEN));

        AccountBlockFacade.BlockOperationResult result = accountBlockFacade.block(1L, 1L);

        assertThat(result.message()).isEqualTo(AccountErrorCode.ACCOUNT_BLOCK_SELF_FORBIDDEN.message());
        verify(accountAuditPublisher).accountBlockFailed(
                eq(1L),
                eq(1L),
                eq(AccountErrorCode.ACCOUNT_BLOCK_SELF_FORBIDDEN.message()),
                any(AppException.class)
        );
    }

    @Test
    void block_rethrowsUnexpectedRuntimeException() {
        when(accountBlockService.block(1L, 2L)).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> accountBlockFacade.block(1L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
    }
}

