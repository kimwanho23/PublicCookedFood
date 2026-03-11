package kwh.PublicCookedFood.notification.service.dispatch;

import kwh.PublicCookedFood.account.domain.Account;

public sealed interface CommentReplyTarget
        permits CommentReplyTarget.Root, CommentReplyTarget.Reply {

    static CommentReplyTarget root() {
        return Root.INSTANCE;
    }

    static CommentReplyTarget reply(Account owner) {
        return new Reply(owner);
    }

    enum Root implements CommentReplyTarget {
        INSTANCE
    }

    record Reply(Account owner) implements CommentReplyTarget {
    }
}
