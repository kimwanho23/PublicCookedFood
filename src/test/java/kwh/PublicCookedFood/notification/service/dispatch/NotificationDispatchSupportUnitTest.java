package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.notification.repository.NotificationRepository;
import kwh.PublicCookedFood.notification.service.NotificationSseService;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private NotificationDispatchSupport notificationDispatchSupport;

    @BeforeEach
    void setUp() {
        notificationDispatchSupport = new NotificationDispatchSupport(
                notificationRepository,
                accountRepository,
                accountBlockService,
                notificationSseService
        );
    }

    @Test
    void resolveMentionedUsers_resolvesMentionByUniqueNickname() {
        when(accountRepository.findByNameIn(Set.of("tester"))).thenReturn(List.of(account()));

        Set<Account> mentionedUsers = notificationDispatchSupport.resolveMentionedUsers("안녕 @tester", 1L);

        assertThat(mentionedUsers).extracting(Account::getId).containsExactly(10L);
    }

    @Test
    void resolveMentionedUsers_ignoresUnknownNickname() {
        when(accountRepository.findByNameIn(Set.of("tester"))).thenReturn(List.of());

        Set<Account> mentionedUsers = notificationDispatchSupport.resolveMentionedUsers("안녕 @tester", 1L);

        assertThat(mentionedUsers).isEmpty();
    }

    private Account account() {
        return Account.builder()
                .id(10L)
                .email("tester" + 10L + "@test.com")
                .name("tester")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }
}
