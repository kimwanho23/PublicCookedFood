package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.account.audit.BoardAuditPublisher;
import kwh.PublicCookedFood.account.audit.BoardCommentAuditPayload;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.policy.BoardAuthorizationPolicy;
import kwh.PublicCookedFood.board.service.BoardCounterService;
import kwh.PublicCookedFood.board.service.BoardScrapService;
import kwh.PublicCookedFood.board.service.CommentNavigationService;
import kwh.PublicCookedFood.board.service.LikeService;
import kwh.PublicCookedFood.board.service.command.BoardReportCommandService;
import kwh.PublicCookedFood.board.service.command.BoardReportCreateCommand;
import kwh.PublicCookedFood.board.service.comment.CommentCommandService;
import kwh.PublicCookedFood.board.service.comment.CommentQueryService;
import kwh.PublicCookedFood.board.service.comment.CommentTargetPath;
import kwh.PublicCookedFood.board.service.comment.CommentTargetPathQuery;
import kwh.PublicCookedFood.board.service.comment.CommentCreateCommand;
import kwh.PublicCookedFood.board.service.comment.CommentDeleteCommand;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.error.ErrorMessageResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class BoardDetailFacade {

    private final CommentCommandService commentCommandService;
    private final CommentQueryService commentQueryService;
    private final BoardCounterService boardCounterService;
    private final BoardScrapService boardScrapService;
    private final BoardReportCommandService boardReportCommandService;
    private final LikeService likeService;
    private final BoardAuditPublisher boardAuditPublisher;
    private final BoardAuthorizationPolicy boardAuthorizationPolicy;
    private final CommentNavigationService commentNavigationService;

    @Transactional
    public OperationResult deleteComment(CommentDeleteCommand command) {
        Comments comment = commentQueryService.getComment(command.commentId());
        if (!boardAuthorizationPolicy.canManageComment(command.actorAccountId(), command.boardId(), comment)) {
            return OperationResult.failure("댓글 삭제 권한이 없습니다.");
        }

        commentCommandService.deleteComment(command.commentId());
        boardCounterService.refreshCommentCount(command.boardId());
        boardAuditPublisher.boardCommentDelete(command.actorAccountId(), command.boardId(), command.commentId());
        return OperationResult.success("댓글이 삭제되었습니다.");
    }

    @Transactional
    public OperationResult addComment(CommentCreateCommand command,
                                      int commentSize) {
        try {
            long savedCommentId = requirePersistedCommentId(commentCommandService.createComment(command));
            boardCounterService.refreshCommentCount(command.boardId());
            BoardCommentAuditPayload auditPayload = command.hasParent()
                    ? BoardCommentAuditPayload.reply(command.boardId(), savedCommentId, command.requiredParentId())
                    : BoardCommentAuditPayload.root(command.boardId(), savedCommentId);
            boardAuditPublisher.boardCommentCreate(command.accountId(), auditPayload);
            CommentTargetPath redirectPath = commentNavigationService.buildCommentTargetPath(
                    CommentTargetPathQuery.of(
                            command.boardId(),
                            savedCommentId,
                            BoardViewer.authenticated(command.accountId()),
                            commentSize
                    )
            );
            return OperationResult.successRedirect(redirectPath.value());
        } catch (AppException e) {
            return failOperation(
                    e,
                    "댓글 등록에 실패했습니다.",
                    message -> boardAuditPublisher.boardCommentCreateFailed(command.accountId(), command.boardId(), message)
            );
        }
    }

    @Transactional
    public void addScrap(Long boardId, Long accountId) {
        boardScrapService.addScrap(boardId, accountId);
        boardAuditPublisher.boardScrapAdd(accountId, boardId);
    }

    @Transactional
    public void removeScrap(Long boardId, Long accountId) {
        boardScrapService.removeScrap(boardId, accountId);
        boardAuditPublisher.boardScrapRemove(accountId, boardId);
    }

    @Transactional
    public OperationResult reportBoard(BoardReportCreateCommand command) {
        try {
            boardReportCommandService.createReport(command);
            boardAuditPublisher.boardReportCreate(command.reporterId(), command.boardId(), command.reason().name());
            return OperationResult.success("신고가 접수되었습니다.");
        } catch (AppException e) {
            return failOperation(
                    e,
                    "신고 처리에 실패했습니다.",
                    message -> boardAuditPublisher.boardReportCreateFailed(command.reporterId(), command.boardId(), message)
            );
        }
    }

    @Transactional
    public void toggleLike(Long boardId, Long accountId) {
        likeService.saveLikes(boardId, accountId);
        boardCounterService.refreshLikes(boardId);
        boardAuditPublisher.boardLikeToggle(accountId, boardId);
    }

    public interface OperationResult {

        Optional<String> message();

        default Optional<String> redirectPath() {
            return Optional.empty();
        }

        default boolean success() {
            return this instanceof Success;
        }

        default String requiredMessage() {
            return message().orElseThrow(() -> new IllegalStateException("operation result message is required"));
        }

        static Success success(String message) {
            return Success.withMessage(normalizeMessage(message));
        }

        static Success successRedirect(String redirectPath) {
            return Success.withRedirectPath(normalizeRedirectPath(redirectPath));
        }

        static Failure failure(String message) {
            return new Failure(normalizeMessage(message));
        }

        final class Success implements OperationResult {

            private final boolean hasMessage;
            private final String message;
            private final boolean hasRedirectPath;
            private final String redirectPath;

            private Success(boolean hasMessage, String message, boolean hasRedirectPath, String redirectPath) {
                this.hasMessage = hasMessage;
                this.message = message;
                this.hasRedirectPath = hasRedirectPath;
                this.redirectPath = redirectPath;
            }

            private static Success withMessage(String message) {
                return new Success(true, message, false, "");
            }

            private static Success withRedirectPath(String redirectPath) {
                return new Success(false, "", true, redirectPath);
            }

            @Override
            public Optional<String> message() {
                return hasMessage ? Optional.of(message) : Optional.empty();
            }

            @Override
            public Optional<String> redirectPath() {
                return hasRedirectPath ? Optional.of(redirectPath) : Optional.empty();
            }
        }

        final class Failure implements OperationResult {

            private final String value;

            private Failure(String value) {
                this.value = value;
            }

            @Override
            public Optional<String> message() {
                return Optional.of(value);
            }
        }
    }

    private OperationResult failOperation(RuntimeException e,
                                          String fallbackMessage,
                                          Consumer<String> failureAudit) {
        String message = ErrorMessageResolver.resolve(e, fallbackMessage);
        markCurrentTransactionForRollback();
        failureAudit.accept(message);
        return OperationResult.failure(message);
    }

    private void markCurrentTransactionForRollback() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
    }

    private static String normalizeMessage(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("operation message must not be blank");
        }
        return value.trim();
    }

    private static String normalizeRedirectPath(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("redirect path must not be blank");
        }
        return value.trim();
    }

    private long requirePersistedCommentId(Comments savedComment) {
        if (savedComment == null || savedComment.getId() == null) {
            throw new IllegalStateException("saved comment id is required");
        }
        return savedComment.getId();
    }
}
