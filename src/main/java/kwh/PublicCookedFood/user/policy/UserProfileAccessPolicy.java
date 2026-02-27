package kwh.PublicCookedFood.user.policy;

import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.service.UserBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserProfileAccessPolicy {

    private final UserBlockService userBlockService;

    public boolean isProfileViewRestricted(Users viewer, Long profileUserId) {
        if (viewer == null || viewer.getId() == null || profileUserId == null) {
            return false;
        }
        if (viewer.getId().equals(profileUserId)) {
            return false;
        }
        return userBlockService.isEitherBlocked(viewer.getId(), profileUserId);
    }

    public boolean hasBlockedProfileUser(Users viewer, Long profileUserId) {
        if (viewer == null || viewer.getId() == null || profileUserId == null) {
            return false;
        }
        if (viewer.getId().equals(profileUserId)) {
            return false;
        }
        return userBlockService.isBlocked(viewer.getId(), profileUserId);
    }
}
