package kwh.PublicCookedFood.account.facade.profile;

import kwh.PublicCookedFood.board.service.BoardScrapService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class ScrapAccountProfileViewStrategy implements AccountProfileViewStrategy {

    private final BoardScrapService boardScrapService;

    @Override
    public AccountProfileViewType viewType() {
        return AccountProfileViewType.SCRAPS;
    }

    @Override
    public AccountProfileViewPages load(Long profileAccountId,
                                     Pageable pageable,
                                     Set<Long> blockedAccountIds) {
        return AccountProfileViewPages.scraps(
                boardScrapService.getScrappedBoardsPage(profileAccountId, pageable, blockedAccountIds)
        );
    }
}

