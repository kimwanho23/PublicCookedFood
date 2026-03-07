package kwh.PublicCookedFood.account.facade.profile;

import org.springframework.data.domain.Pageable;

import java.util.Set;

public interface AccountProfileViewStrategy {

    AccountProfileViewType viewType();

    AccountProfileViewPages load(Long profileAccountId,
                              Pageable pageable,
                              Set<Long> blockedAccountIds);
}

