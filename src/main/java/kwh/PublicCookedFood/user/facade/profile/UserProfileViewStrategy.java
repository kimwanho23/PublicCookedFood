package kwh.PublicCookedFood.user.facade.profile;

import org.springframework.data.domain.Pageable;

import java.util.Set;

public interface UserProfileViewStrategy {

    UserProfileViewType viewType();

    UserProfileViewPages load(Long profileUserId,
                              Pageable pageable,
                              Set<Long> blockedUserIds);
}
