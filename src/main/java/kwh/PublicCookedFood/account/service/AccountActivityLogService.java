package kwh.PublicCookedFood.account.service;

import kwh.PublicCookedFood.account.domain.AccountActivityLog;
import kwh.PublicCookedFood.account.repository.AccountActivityLogRepository;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountActivityLogService {

    private static final int DEFAULT_RECENT_LIMIT = 10;
    private static final int MAX_RECENT_LIMIT = 50;

    private final AccountActivityLogRepository accountActivityLogRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public void record(Long accountId, String action, String detail) {
        if (accountId == null || action == null || action.isBlank()) {
            return;
        }
        accountRepository.findById(accountId).ifPresent(account -> accountActivityLogRepository.save(
                AccountActivityLog.builder()
                        .account(account)
                        .action(action)
                        .detail(detail)
                        .build()
        ));
    }

    @Transactional(readOnly = true)
    public List<AccountActivityLog> getRecentActivities(int limit) {
        int normalizedLimit = limit <= 0 ? DEFAULT_RECENT_LIMIT : Math.min(limit, MAX_RECENT_LIMIT);
        return accountActivityLogRepository.findRecentWithAccount(PageRequest.of(0, normalizedLimit));
    }
}

