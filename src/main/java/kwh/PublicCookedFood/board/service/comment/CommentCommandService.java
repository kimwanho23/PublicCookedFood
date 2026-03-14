package kwh.PublicCookedFood.board.service.comment;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.CommentsRepository;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.error.CommonErrorCode;
import kwh.PublicCookedFood.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CommentCommandService {

    private final CommentsRepository commentsRepository;
    private final BoardRepository boardRepository;
    private final AccountRepository accountRepository;
    private final NotificationService notificationService;
    private final CommentCreatePolicy commentCreatePolicy;
    private final CommentPathResolver commentPathResolver;

    @Transactional
    public void deleteComment(long id) {
        commentsRepository.updateState(id, SoftDeleteState.DELETED);
    }

    @Transactional
    public Comments createComment(CommentCreateCommand command) {
        ResolvedCommentParent parent = resolveParent(command.target());
        commentCreatePolicy.validateParent(parent, command.boardId());

        Account account = accountRepository.findById(command.accountId())
                .orElseThrow(() -> new AppException(CommonErrorCode.RESOURCE_NOT_FOUND, "유효하지 않은 사용자입니다."));
        Board board = boardRepository.findByIdWithAccountAndState(command.boardId(), SoftDeleteState.ACTIVE)
                .orElseThrow(() -> new AppException(CommonErrorCode.INVALID_REQUEST, "삭제된 게시글에는 댓글을 작성할 수 없습니다."));

        commentCreatePolicy.validateActorVisibility(account, board, parent);

        CommentActorBoardContext actorBoardContext = CommentActorBoardContext.of(account, board);
        CommentThreadContext context = CommentThreadContext.forCreate(actorBoardContext, parent);
        Comments comment = context.newComment(command.contents());
        Comments savedComment = commentsRepository.save(comment);
        long savedCommentId = Objects.requireNonNull(savedComment.getId(), "savedComment.id");
        savedComment.initializeThreadMetadata(
                context.initialRootParentId(savedCommentId),
                savedComment.getDepth(),
                commentPathResolver.buildPath(savedCommentId, parent)
        );
        notificationService.notifyOnNewComment(savedComment);
        return savedComment;
    }

    private ResolvedCommentParent resolveParent(CommentParentTarget target) {
        if (target.isReply()) {
            Comments parent = commentsRepository.findById(target.requireParentId())
                    .orElseThrow(() -> new AppException(CommonErrorCode.RESOURCE_NOT_FOUND, "부모 댓글을 찾을 수 없습니다."));
            return ResolvedCommentParent.reply(parent);
        }
        return ResolvedCommentParent.root();
    }
}
