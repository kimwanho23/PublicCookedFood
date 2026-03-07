package kwh.PublicCookedFood.account.policy;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountProfileAccessPolicy {

    private final AccountBlockService accountBlockService;

    public boolean isProfileViewRestricted(Account viewer, Long profileAccountId) {
        if (viewer == null || viewer.getId() == null || profileAccountId == null) {
            return false;
        }
        if (viewer.getId().equals(profileAccountId)) {
            return false;
        }
        return accountBlockService.isEitherBlocked(viewer.getId(), profileAccountId);
    }

    public boolean hasBlockedProfileAccount(Account viewer, Long profileAccountId) {
        if (viewer == null || viewer.getId() == null || profileAccountId == null) {
            return false;
        }
        if (viewer.getId().equals(profileAccountId)) {
            return false;
        }
        return accountBlockService.isBlocked(viewer.getId(), profileAccountId);
    }
}

