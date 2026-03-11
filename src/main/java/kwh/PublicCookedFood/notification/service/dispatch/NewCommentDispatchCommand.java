package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.board.domain.Comments;

public record NewCommentDispatchCommand(
        Comments comment,
        CommentReplyTarget replyTarget) {

    public static NewCommentDispatchCommand from(Comments comment) {
        Comments parent = comment.getParent();
        if (parent == null) {
            return new NewCommentDispatchCommand(comment, CommentReplyTarget.root());
        }
        return new NewCommentDispatchCommand(comment, CommentReplyTarget.reply(parent.getAccount()));
    }

    public NewCommentDispatchCommand {
        Comments parent = comment.getParent();
        if (parent == null && !(replyTarget instanceof CommentReplyTarget.Root)) {
            throw new IllegalStateException("루트 댓글 알림은 root reply target 이어야 합니다.");
        }
        if (parent != null && !(replyTarget instanceof CommentReplyTarget.Reply reply)) {
            throw new IllegalStateException("답글 알림은 reply target 이어야 합니다.");
        }
        if (parent != null) {
            CommentReplyTarget.Reply reply = (CommentReplyTarget.Reply) replyTarget;
            if (!NotificationReceiverPolicy.isSameAccount(parent.getAccount(), reply.owner())) {
                throw new IllegalStateException("답글 알림 대상이 부모 댓글 작성자와 일치해야 합니다.");
            }
        }
    }
}
