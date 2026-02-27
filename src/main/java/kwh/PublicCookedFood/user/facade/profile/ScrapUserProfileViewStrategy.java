package kwh.PublicCookedFood.user.facade.profile;

import kwh.PublicCookedFood.board.service.BoardScrapService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class ScrapUserProfileViewStrategy implements UserProfileViewStrategy {

    private final BoardScrapService boardScrapService;

    @Override
    public UserProfileViewType viewType() {
        return UserProfileViewType.SCRAPS;
    }

    @Override
    public UserProfileViewPages load(Long profileUserId,
                                     Pageable pageable,
                                     Set<Long> blockedUserIds) {
        return UserProfileViewPages.scraps(
                boardScrapService.getScrappedBoardsPage(profileUserId, pageable, blockedUserIds)
        );
    }
}
