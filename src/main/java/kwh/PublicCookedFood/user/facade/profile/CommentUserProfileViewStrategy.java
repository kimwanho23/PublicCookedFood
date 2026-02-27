package kwh.PublicCookedFood.user.facade.profile;

import kwh.PublicCookedFood.board.service.CommentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class CommentUserProfileViewStrategy implements UserProfileViewStrategy {

    private final CommentsService commentsService;

    @Override
    public UserProfileViewType viewType() {
        return UserProfileViewType.COMMENTS;
    }

    @Override
    public UserProfileViewPages load(Long profileUserId,
                                     Pageable pageable,
                                     Set<Long> blockedUserIds) {
        return UserProfileViewPages.comments(
                commentsService.getUserCommentPage(profileUserId, pageable, blockedUserIds)
        );
    }
}
