package kwh.PublicCookedFood.board.service.comment;

import kwh.PublicCookedFood.account.service.AccountBlockService;
import kwh.PublicCookedFood.board.application.query.view.CommentNodeView;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.board.facade.BoardViewer;
import kwh.PublicCookedFood.board.repository.CommentsRepository;
import kwh.PublicCookedFood.board.service.support.BoardVisibilityCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.error.CommonErrorCode;

@Service
@RequiredArgsConstructor
public class CommentQueryService {

    private final CommentsRepository commentsRepository;
    private final AccountBlockService accountBlockService;
    private final CommentReplyLoader commentReplyLoader;
    private final CommentTreeAssembler commentTreeAssembler;
    private final VisibleRootCommentScanner visibleRootCommentScanner;

    @Transactional(readOnly = true)
    public Comments getComment(long id) {
        return commentsRepository.findById(id)
                .orElseThrow(() -> new AppException(CommonErrorCode.RESOURCE_NOT_FOUND, "댓글을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public long getCommentsCount(long boardId, BoardViewer viewer) {
        Set<Long> blockedAccountIds = restrictedAccountIds(viewer);
        if (blockedAccountIds.isEmpty()) {
            return commentsRepository.countByBoardIdAndState(boardId, SoftDeleteState.ACTIVE);
        }
        return commentsRepository.countByBoardIdAndStateAndAccountIdNotIn(boardId, SoftDeleteState.ACTIVE, blockedAccountIds);
    }

    @Transactional(readOnly = true)
    public Page<CommentNodeView> getCommentListWithReplies(long boardId, Pageable pageable, BoardViewer viewer) {
        Set<Long> blockedAccountIds = restrictedAccountIds(viewer);
        BoardVisibilityCriteria visibility = BoardVisibilityCriteria.of(blockedAccountIds);
        Pageable effectivePageable = normalizePageable(pageable);

        VisibleRootCommentScanResult scanResult = scanVisibleRootComments(boardId, visibility, effectivePageable);
        List<Comments> selectedParentComments = scanResult.selectedVisibleRootComments();
        Map<Long, List<Comments>> pageRepliesByParentId =
                commentReplyLoader.loadRepliesByRootParentIds(boardId, selectedParentComments, visibility);
        List<CommentNodeView> content = selectedParentComments.stream()
                .map(comment -> commentTreeAssembler.toView(comment, pageRepliesByParentId, boardId))
                .collect(Collectors.toList());
        Pageable resultPageable = effectivePageable.isUnpaged() ? Pageable.unpaged() : effectivePageable;
        return new PageImpl<>(content, resultPageable, scanResult.totalVisibleRootCount());
    }

    @Transactional(readOnly = true)
    public Page<Comments> getAccountCommentPage(Long accountId, Pageable pageable, Set<Long> blockedAccountIds) {
        if (accountId == null) {
            return Page.empty(pageable);
        }
        BoardVisibilityCriteria visibility = BoardVisibilityCriteria.of(blockedAccountIds);
        return commentsRepository.findAccountCommentsWithBoard(
                accountId,
                SoftDeleteState.ACTIVE,
                SoftDeleteState.ACTIVE,
                visibility.excludeRestricted(),
                visibility.restrictedAccountIdsOrSentinel(),
                pageable
        );
    }

    private Pageable normalizePageable(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return Pageable.unpaged();
        }
        return PageRequest.of(
                Math.max(pageable.getPageNumber(), 0),
                Math.max(pageable.getPageSize(), 1),
                pageable.getSort()
        );
    }

    private VisibleRootCommentScanResult scanVisibleRootComments(long boardId,
                                                                 BoardVisibilityCriteria visibility,
                                                                 Pageable effectivePageable) {
        if (effectivePageable.isUnpaged()) {
            return visibleRootCommentScanner.scan(VisibleRootCommentScanQuery.all(boardId, visibility));
        }
        long pageStartIndex = effectivePageable.getOffset();
        long pageEndExclusive = pageStartIndex + effectivePageable.getPageSize();
        return visibleRootCommentScanner.scan(
                VisibleRootCommentScanQuery.page(boardId, visibility, pageStartIndex, pageEndExclusive)
        );
    }

    private Set<Long> restrictedAccountIds(BoardViewer viewer) {
        Objects.requireNonNull(viewer, "viewer");
        return viewer.maybeAccountId()
                .map(accountBlockService::getViewRestrictedAccountIds)
                .orElseGet(Collections::emptySet);
    }
}
