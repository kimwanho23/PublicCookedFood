package kwh.PublicCookedFood.account.service;

import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.error.AccountErrorCode;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceIntegrationTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    @DisplayName("회원가입 테스트")
    void saveMemberTest() {
        Account account = createAccount("test@email.com");
        when(accountRepository.findByEmail("test@email.com")).thenReturn(Optional.empty());
        when(accountRepository.findByName("홍길동")).thenReturn(Optional.empty());
        when(accountRepository.saveAndFlush(account)).thenReturn(account);

        Account savedMember = accountService.save(account);

        assertThat(savedMember.getEmail()).isEqualTo("test@email.com");
        verify(accountRepository).findByEmail("test@email.com");
        verify(accountRepository).findByName("홍길동");
        verify(accountRepository).saveAndFlush(account);
    }

    @Test
    void saveMemberTest_throwsWhenDuplicateEmailExists() {
        Account existing = createAccount("dup@email.com");
        Account incoming = createAccount("dup@email.com");
        when(accountRepository.findByEmail("dup@email.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> accountService.save(incoming))
                .isInstanceOfSatisfying(AppException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(AccountErrorCode.ACCOUNT_EMAIL_DUPLICATED);
                    assertThat(e.getMessage()).contains("이미 가입된 이메일");
                });
    }

    @Test
    void saveMemberTest_throwsWhenDuplicateNameExists() {
        Account existing = createAccount("existing@email.com");
        Account incoming = createAccount("incoming@email.com");
        when(accountRepository.findByEmail("incoming@email.com")).thenReturn(Optional.empty());
        when(accountRepository.findByName("홍길동")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> accountService.save(incoming))
                .isInstanceOfSatisfying(AppException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(AccountErrorCode.ACCOUNT_NAME_DUPLICATED);
                    assertThat(e.getMessage()).contains("이미 사용 중인 닉네임");
                });
    }

    private Account createAccount(String email) {
        return Account.builder()
                .email(email)
                .name("홍길동")
                .password("encoded-password")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }
}
