package kwh.PublicCookedFood.user.facade.profile;

import kwh.PublicCookedFood.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class BoardUserProfileViewStrategy implements UserProfileViewStrategy {

    private final BoardService boardService;

    @Override
    public UserProfileViewType viewType() {
        return UserProfileViewType.BOARDS;
    }

    @Override
    public UserProfileViewPages load(Long profileUserId,
                                     Pageable pageable,
                                     Set<Long> blockedUserIds) {
        return UserProfileViewPages.boards(
                boardService.getBoardList(pageable, null, profileUserId, blockedUserIds)
        );
    }
}
