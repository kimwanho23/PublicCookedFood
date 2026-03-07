package kwh.PublicCookedFood.account.policy;

import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountProfileAccessPolicyUnitTest {

    @Mock
    private AccountBlockService accountBlockService;

    @InjectMocks
    private AccountProfileAccessPolicy accountProfileAccessPolicy;

    @Test
    void isProfileViewRestricted_returnsFalseForSelfProfile() {
        Account viewer = createAccount(11L);

        boolean restricted = accountProfileAccessPolicy.isProfileViewRestricted(viewer, 11L);

        assertThat(restricted).isFalse();
    }

    @Test
    void isProfileViewRestricted_delegatesToBlockServiceForOtherUser() {
        Account viewer = createAccount(11L);
        when(accountBlockService.isEitherBlocked(11L, 22L)).thenReturn(true);

        boolean restricted = accountProfileAccessPolicy.isProfileViewRestricted(viewer, 22L);

        assertThat(restricted).isTrue();
        verify(accountBlockService).isEitherBlocked(11L, 22L);
    }

    @Test
    void hasBlockedProfileAccount_delegatesToBlockServiceForOtherUser() {
        Account viewer = createAccount(11L);
        when(accountBlockService.isBlocked(11L, 22L)).thenReturn(true);

        boolean blocked = accountProfileAccessPolicy.hasBlockedProfileAccount(viewer, 22L);

        assertThat(blocked).isTrue();
        verify(accountBlockService).isBlocked(11L, 22L);
    }

    @Test
    void hasBlockedProfileAccount_returnsFalseForNullViewer() {
        boolean blocked = accountProfileAccessPolicy.hasBlockedProfileAccount(null, 22L);

        assertThat(blocked).isFalse();
    }

    private Account createAccount(Long id) {
        return Account.builder()
                .id(id)
                .email("viewer-" + id + "@test.com")
                .name("viewer")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }
}
