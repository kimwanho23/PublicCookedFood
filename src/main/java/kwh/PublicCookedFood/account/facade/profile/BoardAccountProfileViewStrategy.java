package kwh.PublicCookedFood.account.facade.profile;

import kwh.PublicCookedFood.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class BoardAccountProfileViewStrategy implements AccountProfileViewStrategy {

    private final BoardService boardService;

    @Override
    public AccountProfileViewType viewType() {
        return AccountProfileViewType.BOARDS;
    }

    @Override
    public AccountProfileViewPages load(Long profileAccountId,
                                     Pageable pageable,
                                     Set<Long> blockedAccountIds) {
        return AccountProfileViewPages.boards(
                boardService.getBoardList(pageable, null, profileAccountId, blockedAccountIds)
        );
    }
}

