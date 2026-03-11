package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationMentionResolver {

    private final MentionNameExtractor mentionNameExtractor;
    private final AccountRepository accountRepository;

    public List<Account> resolveMentionedUsers(String rawText, long actorAccountId) {
        List<String> mentionNames = mentionNameExtractor.extract(rawText);
        if (mentionNames.isEmpty()) {
            return List.of();
        }
        MentionedAccounts mentionedAccounts = loadMentionedAccounts(mentionNames);
        return mentionedAccounts.resolveRecipients(mentionNames, actorAccountId);
    }

    private MentionedAccounts loadMentionedAccounts(List<String> mentionNames) {
        List<Account> accounts = accountRepository.findByNameIn(mentionNames);
        return MentionedAccounts.from(accounts);
    }

    private record MentionedAccounts(Map<String, Account> accountsByName) {

        private static MentionedAccounts from(List<Account> accounts) {
                Map<String, Account> accountsByName = new LinkedHashMap<>();
                for (Account account : accounts) {
                    accountsByName.putIfAbsent(account.getName(), account);
                }
                return new MentionedAccounts(accountsByName);
            }

            private List<Account> resolveRecipients(List<String> mentionNames, long actorAccountId) {
                List<Account> resolvedUsers = new ArrayList<>();
                for (String mentionName : mentionNames) {
                    if (!accountsByName.containsKey(mentionName)) {
                        continue;
                    }
                    Account candidate = accountsByName.get(mentionName);
                    if (candidate.getId().equals(actorAccountId)) {
                        continue;
                    }
                    resolvedUsers.add(candidate);
                }
                return resolvedUsers;
            }
        }
}
