package kwh.PublicCookedFood.account.facade.profile;

import kwh.PublicCookedFood.board.service.comment.CommentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class CommentAccountProfileViewStrategy implements AccountProfileViewStrategy {

    private final CommentQueryService commentQueryService;

    @Override
    public AccountProfileViewType viewType() {
        return AccountProfileViewType.COMMENTS;
    }

    @Override
    public AccountProfileViewPages load(Long profileAccountId,
                                     Pageable pageable,
                                     Set<Long> blockedAccountIds) {
        return AccountProfileViewPages.comments(
                commentQueryService.getAccountCommentPage(profileAccountId, pageable, blockedAccountIds)
        );
    }
}

