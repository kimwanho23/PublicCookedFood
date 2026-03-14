package kwh.PublicCookedFood.account.facade.profile;

import kwh.PublicCookedFood.board.service.query.BoardListCriteria;
import kwh.PublicCookedFood.board.service.query.BoardListQueryService;
import kwh.PublicCookedFood.board.service.support.BoardVisibilityCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class BoardAccountProfileViewStrategy implements AccountProfileViewStrategy {

    private final BoardListQueryService boardListQueryService;

    @Override
    public AccountProfileViewType viewType() {
        return AccountProfileViewType.BOARDS;
    }

    @Override
    public AccountProfileViewPages load(Long profileAccountId,
                                     Pageable pageable,
                                     Set<Long> blockedAccountIds) {
        return AccountProfileViewPages.boards(
                boardListQueryService.load(BoardListCriteria.forAuthor(
                        pageable,
                        profileAccountId,
                        BoardVisibilityCriteria.of(blockedAccountIds)
                ))
        );
    }
}

