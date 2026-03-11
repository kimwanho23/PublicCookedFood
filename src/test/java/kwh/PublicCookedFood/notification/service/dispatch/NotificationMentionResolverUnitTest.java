package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationMentionResolverUnitTest {

    @Mock
    private AccountRepository accountRepository;

    private NotificationMentionResolver notificationMentionResolver;

    @BeforeEach
    void setUp() {
        notificationMentionResolver = new NotificationMentionResolver(
                new MentionNameExtractor(),
                accountRepository
        );
    }

    @Test
    void resolveMentionedUsers_resolvesMentionByUniqueNickname() {
        when(accountRepository.findByNameIn(List.of("tester"))).thenReturn(List.of(account()));

        List<Account> mentionedUsers = notificationMentionResolver.resolveMentionedUsers("안녕 @tester", 1L);

        assertThat(mentionedUsers).extracting(Account::getId).containsExactly(10L);
    }

    @Test
    void resolveMentionedUsers_ignoresUnknownNickname() {
        when(accountRepository.findByNameIn(List.of("tester"))).thenReturn(List.of());

        List<Account> mentionedUsers = notificationMentionResolver.resolveMentionedUsers("안녕 @tester", 1L);

        assertThat(mentionedUsers).isEmpty();
    }

    @Test
    void resolveMentionedUsers_excludesActorAndPreservesMentionOrder() {
        when(accountRepository.findByNameIn(List.of("beta", "actor", "alpha"))).thenReturn(List.of(
                account(30L, "actor"),
                account(10L, "alpha"),
                account(20L, "beta")
        ));

        List<Account> mentionedUsers = notificationMentionResolver.resolveMentionedUsers("안녕 @beta @actor @alpha", 30L);

        assertThat(mentionedUsers).extracting(Account::getId).containsExactly(20L, 10L);
    }

    private Account account() {
        return account(10L, "tester");
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
