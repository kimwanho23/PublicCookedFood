package kwh.PublicCookedFood.account.service;

import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.domain.AccountBlock;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.error.AccountErrorCode;
import kwh.PublicCookedFood.account.repository.AccountBlockRepository;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountBlockServiceUnitTest {

    @Mock
    private AccountBlockRepository accountBlockRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountBlockService accountBlockService;

    @Test
    void block_returnsFalseWhenAlreadyBlocked() {
        when(accountBlockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).thenReturn(true);

        boolean created = accountBlockService.block(1L, 2L);

        assertThat(created).isFalse();
        verify(accountBlockRepository, never()).save(any(AccountBlock.class));
    }

    @Test
    void block_throwsWhenSelfBlockRequested() {
        assertThatThrownBy(() -> accountBlockService.block(1L, 1L))
                .isInstanceOfSatisfying(AppException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AccountErrorCode.ACCOUNT_BLOCK_SELF_FORBIDDEN));
    }

    @Test
    void block_throwsAppExceptionWhenTargetAccountDoesNotExist() {
        Account blocker = createAccount(1L, "blocker@test.com");
        when(accountBlockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).thenReturn(false);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(blocker));
        when(accountRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountBlockService.block(1L, 2L))
                .isInstanceOfSatisfying(AppException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AccountErrorCode.ACCOUNT_NOT_FOUND));
    }

    @Test
    void unblock_returnsFalseWhenNoRowDeleted() {
        when(accountBlockRepository.deleteByBlockerIdAndBlockedId(1L, 2L)).thenReturn(0L);

        boolean removed = accountBlockService.unblock(1L, 2L);

        assertThat(removed).isFalse();
    }

    @Test
    void block_returnsFalseWhenDuplicateConstraintRaisedByRaceCondition() {
        Account blocker = createAccount(1L, "blocker@test.com");
        Account blocked = createAccount(2L, "blocked@test.com");
        when(accountBlockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).thenReturn(false, true);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(blocker));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(blocked));
        when(accountBlockRepository.saveAndFlush(any(AccountBlock.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        boolean created = accountBlockService.block(1L, 2L);

        assertThat(created).isFalse();
    }

    @Test
    void getBlockedAccounts_returnsBlockedAccounts() {
        Account blocker = createAccount(1L, "blocker@test.com");
        Account blockedA = createAccount(2L, "blocked-a@test.com");
        Account blockedB = createAccount(3L, "blocked-b@test.com");

        when(accountBlockRepository.findByBlockerIdOrderByRegTimeDesc(1L)).thenReturn(List.of(
                AccountBlock.of(blocker, blockedA),
                AccountBlock.of(blocker, blockedB)));

        List<Account> blockedAccounts = accountBlockService.getBlockedAccounts(1L);

        assertThat(blockedAccounts).extracting(Account::getId).containsExactlyInAnyOrder(2L, 3L);
        verify(accountBlockRepository).findByBlockerIdOrderByRegTimeDesc(1L);
    }

    private Account createAccount(Long id, String email) {
        return Account.builder()
                .id(id)
                .email(email)
                .name("account")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }
}


