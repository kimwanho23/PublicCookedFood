package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.board.policy.BoardAuthorizationPolicy;
import kwh.PublicCookedFood.board.service.BoardScrapService;
import kwh.PublicCookedFood.board.service.LikeService;
import kwh.PublicCookedFood.board.service.query.BoardReportQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BoardInteractionStateResolver {

    private final LikeService likeService;
    private final BoardScrapService boardScrapService;
    private final BoardReportQueryService boardReportQueryService;
    private final BoardAuthorizationPolicy boardAuthorizationPolicy;

    public BoardInteractionState resolve(Long boardId, Account account, Long authorAccountId) {
        return resolve(boardId, BoardViewer.from(account), authorAccountId);
    }

    public BoardInteractionState resolve(Long boardId, BoardViewer viewer, Long authorAccountId) {
        if (!viewer.isAuthenticated()) {
            return BoardInteractionState.anonymous();
        }

        Long currentAccountId = viewer.requireAuthenticated().accountId();
        boolean myLike = likeService.hasLike(boardId, currentAccountId);
        boolean myScrap = boardScrapService.isScrapped(boardId, currentAccountId);
        boolean myReport = boardReportQueryService.hasReported(boardId, currentAccountId);
        boolean myBlockedAuthor = boardAuthorizationPolicy.isAuthorBlockedByViewer(viewer, authorAccountId);
        return BoardInteractionState.authenticated(
                viewer.requireAuthenticated(),
                myLike,
                myScrap,
                myReport,
                myBlockedAuthor
        );
    }
}
