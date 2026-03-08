package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.notification.repository.NotificationRepository;
import kwh.PublicCookedFood.notification.service.NotificationSseService;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchSupportUnitTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountBlockService accountBlockService;

    @Mock
    private NotificationSseService notificationSseService;

    @InjectMocks
    private NotificationDispatchSupport notificationDispatchSupport;

    @Test
    void resolveMentionedUsers_resolvesMentionByUniqueNickname() {
        when(accountRepository.findByNameIn(Set.of("tester"))).thenReturn(List.of(account(10L, "tester")));

        Set<Account> mentionedUsers = notificationDispatchSupport.resolveMentionedUsers("안녕 @tester", 1L);

        assertThat(mentionedUsers).extracting(Account::getId).containsExactly(10L);
    }

    @Test
    void resolveMentionedUsers_ignoresUnknownNickname() {
        when(accountRepository.findByNameIn(Set.of("tester"))).thenReturn(List.of());

        Set<Account> mentionedUsers = notificationDispatchSupport.resolveMentionedUsers("안녕 @tester", 1L);

        assertThat(mentionedUsers).isEmpty();
    }

    private Account account(Long id, String name) {
        return Account.builder()
                .id(id)
                .email(name + id + "@test.com")
                .name(name)
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }
}
