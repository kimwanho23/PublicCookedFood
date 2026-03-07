package kwh.PublicCookedFood.account.aop.action;

import kwh.PublicCookedFood.account.aop.AccountActionAuditType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AccountActionStrategiesUnitTest {

    @Mock
    private AccountActionAuditRecorder recorder;

    private AccountProfileAuditStrategy accountProfileAuditStrategy;
    private AccountBlockAuditStrategy accountBlockAuditStrategy;
    private BookmarkAuditStrategy bookmarkAuditStrategy;
    private RecipeReviewAuditStrategy recipeReviewAuditStrategy;
    private BoardCommentAuditStrategy boardCommentAuditStrategy;
    private BoardLifecycleAuditStrategy boardLifecycleAuditStrategy;
    private BoardReportAuditStrategy boardReportAuditStrategy;
    private NotificationAuditStrategy notificationAuditStrategy;

    @BeforeEach
    void setUp() {
        accountProfileAuditStrategy = new AccountProfileAuditStrategy(recorder);
        accountBlockAuditStrategy = new AccountBlockAuditStrategy(recorder);
        bookmarkAuditStrategy = new BookmarkAuditStrategy(recorder);
        recipeReviewAuditStrategy = new RecipeReviewAuditStrategy(recorder);
        boardCommentAuditStrategy = new BoardCommentAuditStrategy(recorder);
        boardLifecycleAuditStrategy = new BoardLifecycleAuditStrategy(recorder);
        boardReportAuditStrategy = new BoardReportAuditStrategy(recorder);
        notificationAuditStrategy = new NotificationAuditStrategy(recorder);
    }

    @Test
    void accountProfileStrategy_signup_recordsActivity() {
        accountProfileAuditStrategy.handle(AccountActionAuditType.ACCOUNT_SIGNUP,
                new AccountActionAuditArgs(new Object[]{1L, "tester@example.com"}));

        verify(recorder).record(1L, "ACCOUNT_SIGNUP", "email=tester@example.com");
    }

    @Test
    void accountProfileStrategy_profileUpdateFailure_doesNotRecordActivity() {
        accountProfileAuditStrategy.handle(AccountActionAuditType.ACCOUNT_PROFILE_UPDATE_FAILED_UNAUTHENTICATED,
                new AccountActionAuditArgs(new Object[]{}));

        verifyNoInteractions(recorder);
    }

    @Test
    void accountBlockStrategy_blockSuccess_recordsActivity() {
        accountBlockAuditStrategy.handle(AccountActionAuditType.ACCOUNT_BLOCK_ADD,
                new AccountActionAuditArgs(new Object[]{3L, 9L}));

        verify(recorder).record(3L, "ACCOUNT_BLOCK_ADD", "targetAccountId=9");
    }

    @Test
    void accountBlockStrategy_blockFailure_doesNotRecordActivity() {
        accountBlockAuditStrategy.handle(AccountActionAuditType.ACCOUNT_BLOCK_FAILED,
                new AccountActionAuditArgs(new Object[]{3L, 9L, "duplicate", new IllegalStateException("boom")}));

        verifyNoInteractions(recorder);
    }

    @Test
    void bookmarkStrategy_addSuccess_recordsActivity() {
        bookmarkAuditStrategy.handle(AccountActionAuditType.BOOKMARK_ADD,
                new AccountActionAuditArgs(new Object[]{5L, 13L}));

        verify(recorder).record(5L, "BOOKMARK_ADD", "recipeId=13");
    }

    @Test
    void bookmarkStrategy_addFailure_doesNotRecordActivity() {
        bookmarkAuditStrategy.handle(AccountActionAuditType.BOOKMARK_ADD_FAILED,
                new AccountActionAuditArgs(new Object[]{5L, 13L, "already-bookmarked"}));

        verifyNoInteractions(recorder);
    }

    @Test
    void recipeReviewStrategy_upsertSuccess_recordsActivity() {
        recipeReviewAuditStrategy.handle(AccountActionAuditType.RECIPE_REVIEW_UPSERT,
                new AccountActionAuditArgs(new Object[]{7L, 21L, 4}));

        verify(recorder).record(7L, "RECIPE_REVIEW_UPSERT", "recipeId=21,rating=4");
    }

    @Test
    void recipeReviewStrategy_upsertFailure_doesNotRecordActivity() {
        recipeReviewAuditStrategy.handle(AccountActionAuditType.RECIPE_REVIEW_UPSERT_FAILED,
                new AccountActionAuditArgs(new Object[]{7L, 21L, "validation-failed"}));

        verifyNoInteractions(recorder);
    }

    @Test
    void boardCommentStrategy_commentCreateSuccess_recordsActivityWithParentIdFallback() {
        boardCommentAuditStrategy.handle(AccountActionAuditType.BOARD_COMMENT_CREATE,
                new AccountActionAuditArgs(new Object[]{11L, 22L, 33L, null}));

        verify(recorder).record(11L, "BOARD_COMMENT_CREATE", "boardId=22,commentId=33,parentId=-");
    }

    @Test
    void boardLifecycleStrategy_createSuccess_recordsActivity() {
        boardLifecycleAuditStrategy.handle(AccountActionAuditType.BOARD_CREATE,
                new AccountActionAuditArgs(new Object[]{11L, 77L}));

        verify(recorder).record(11L, "BOARD_CREATE", "boardId=77");
    }

    @Test
    void boardReportStrategy_reportCreateFailure_doesNotRecordActivity() {
        boardReportAuditStrategy.handle(AccountActionAuditType.BOARD_REPORT_CREATE_FAILED,
                new AccountActionAuditArgs(new Object[]{11L, 22L, "validation-failed"}));

        verifyNoInteractions(recorder);
    }

    @Test
    void notificationStrategy_settingUpdate_recordsActivityWithSafeBooleanFallback() {
        notificationAuditStrategy.handle(AccountActionAuditType.NOTIFICATION_SETTING_UPDATE,
                new AccountActionAuditArgs(new Object[]{15L, null}));

        verify(recorder).record(15L, "NOTIFICATION_SETTING_UPDATE", "enabled=-");
    }
}


